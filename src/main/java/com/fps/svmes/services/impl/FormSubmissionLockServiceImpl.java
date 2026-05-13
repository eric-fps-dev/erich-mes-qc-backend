package com.fps.svmes.services.impl;

import com.fps.svmes.dto.requests.FormSubmissionLockAcquireRequest;
import com.fps.svmes.dto.requests.FormSubmissionLockHeartbeatRequest;
import com.fps.svmes.dto.requests.FormSubmissionLockReleaseRequest;
import com.fps.svmes.dto.requests.FormSubmissionLockStatusRequest;
import com.fps.svmes.dto.responses.FormSubmissionLockResponse;
import com.fps.svmes.enums.form.FormSubmissionLockStatus;
import com.fps.svmes.models.nosql.FormSubmissionLock;
import com.fps.svmes.repositories.mongoRepo.FormSubmissionLockRepository;
import com.fps.svmes.services.FormSubmissionLockService;
import com.fps.svmes.services.FormSubmissionMutationAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FormSubmissionLockServiceImpl implements FormSubmissionLockService {
    private final MongoTemplate mongoTemplate;
    private final FormSubmissionLockRepository formSubmissionLockRepository;
    private final FormSubmissionMutationAccessService mutationAccessService;

    @Value("${qc.form-submission-lock.ttl-seconds:60}")
    private long lockTtlSeconds;

    @Override
    public FormSubmissionLockResponse acquireLock(FormSubmissionLockAcquireRequest request) {
        var target = mutationAccessService.resolveTarget(request.getSubmissionId(), request.getCollectionName(), request.getLockPurpose());
        if (!mutationAccessService.canAccess(target,
                request.getActorUserId(),
                null,
                request.getActorRoleIds(),
                request.getLockPurpose())) {
            return buildResponse(null, target.submissionId(), target.collectionName(), FormSubmissionLockStatus.READ_ONLY_NOT_ELIGIBLE,
                    request.getLockPurpose(), false, true);
        }

        Date now = Date.from(Instant.now());
        FormSubmissionLock existing = findLock(target.submissionId(), target.collectionName());
        if (existing != null && !isExpired(existing, now)) {
            if (ownedBy(existing, request.getActorUserId(), request.getSessionId())) {
                return buildResponse(existing, target.submissionId(), target.collectionName(), FormSubmissionLockStatus.ALREADY_OWNED,
                        existing.getLockPurpose(), true, false);
            }
            if (sameUser(existing, request.getActorUserId())) {
                if (Boolean.TRUE.equals(request.getTakeOverExistingLock())) {
                    FormSubmissionLock takenOver = takeOverLock(existing, request, target.collectionName(), now);
                    return buildResponse(takenOver, target.submissionId(), target.collectionName(), FormSubmissionLockStatus.ACQUIRED,
                            takenOver.getLockPurpose(), true, false);
                }
                return buildResponse(existing, target.submissionId(), target.collectionName(), FormSubmissionLockStatus.ALREADY_OWNED,
                        existing.getLockPurpose(), true, false);
            }
            return buildResponse(existing, target.submissionId(), target.collectionName(), FormSubmissionLockStatus.LOCKED_BY_OTHER,
                    existing.getLockPurpose(), false, true);
        }

        try {
            FormSubmissionLock acquired = acquireFreshLock(request, target.collectionName(), now);
            return buildResponse(acquired, target.submissionId(), target.collectionName(), FormSubmissionLockStatus.ACQUIRED,
                    acquired.getLockPurpose(), true, false);
        } catch (DuplicateKeyException ex) {
            FormSubmissionLock current = findLock(target.submissionId(), target.collectionName());
            if (current != null && !isExpired(current, now)) {
                if (ownedBy(current, request.getActorUserId(), request.getSessionId())) {
                    return buildResponse(current, target.submissionId(), target.collectionName(), FormSubmissionLockStatus.ALREADY_OWNED,
                            current.getLockPurpose(), true, false);
                }
                if (sameUser(current, request.getActorUserId())) {
                    return buildResponse(current, target.submissionId(), target.collectionName(), FormSubmissionLockStatus.ALREADY_OWNED,
                            current.getLockPurpose(), true, false);
                }
                return buildResponse(current, target.submissionId(), target.collectionName(), FormSubmissionLockStatus.LOCKED_BY_OTHER,
                        current.getLockPurpose(), false, true);
            }
            throw ex;
        }
    }

    @Override
    public FormSubmissionLockResponse renewLock(FormSubmissionLockHeartbeatRequest request) {
        Date now = Date.from(Instant.now());
        Query query = new Query(Criteria.where("submissionId").is(request.getSubmissionId())
                .and("collectionName").is(request.getCollectionName())
                .and("lockedByUserId").is(request.getActorUserId())
                .and("lockToken").is(request.getLockToken())
                .and("expiresAt").gt(now));
        Update update = new Update()
                .set("expiresAt", expiryFrom(now))
                .set("updatedAt", now);
        if (request.getSessionId() != null && !request.getSessionId().isBlank()) {
            update.set("sessionId", request.getSessionId());
        }
        FormSubmissionLock renewed = mongoTemplate.findAndModify(query, update,
                FindAndModifyOptions.options().returnNew(true), FormSubmissionLock.class);
        if (renewed == null) {
            FormSubmissionLock current = findLock(request.getSubmissionId(), request.getCollectionName());
            return buildResponse(current, request.getSubmissionId(), request.getCollectionName(), FormSubmissionLockStatus.NOT_LOCKED,
                    current == null ? null : current.getLockPurpose(), false, false);
        }
        return buildResponse(renewed, request.getSubmissionId(), request.getCollectionName(), FormSubmissionLockStatus.ACQUIRED,
                renewed.getLockPurpose(), true, false);
    }

    @Override
    public FormSubmissionLockResponse releaseLock(FormSubmissionLockReleaseRequest request) {
        Query query = new Query(Criteria.where("submissionId").is(request.getSubmissionId())
                .and("collectionName").is(request.getCollectionName())
                .and("lockedByUserId").is(request.getActorUserId())
                .and("lockToken").is(request.getLockToken()));
        FormSubmissionLock released = mongoTemplate.findAndRemove(query, FormSubmissionLock.class);
        if (released == null) {
            return buildResponse(null, request.getSubmissionId(), request.getCollectionName(), FormSubmissionLockStatus.RELEASED,
                    null, false, false);
        }
        return buildResponse(released, request.getSubmissionId(), request.getCollectionName(), FormSubmissionLockStatus.RELEASED,
                released.getLockPurpose(), false, false);
    }

    @Override
    public FormSubmissionLockResponse getLockStatus(FormSubmissionLockStatusRequest request) {
        var target = mutationAccessService.resolveTarget(request.getSubmissionId(), request.getCollectionName(), request.getLockPurpose());
        if (!mutationAccessService.canAccess(target,
                request.getActorUserId(),
                null,
                request.getActorRoleIds(),
                request.getLockPurpose())) {
            return buildResponse(null, target.submissionId(), target.collectionName(), FormSubmissionLockStatus.READ_ONLY_NOT_ELIGIBLE,
                    request.getLockPurpose(), false, true);
        }
        FormSubmissionLock existing = findLock(target.submissionId(), target.collectionName());
        Date now = Date.from(Instant.now());
        if (existing == null || isExpired(existing, now)) {
            return buildResponse(null, target.submissionId(), target.collectionName(), FormSubmissionLockStatus.NOT_LOCKED,
                    request.getLockPurpose(), false, false);
        }
        if ((request.getSessionId() != null && ownedBy(existing, request.getActorUserId(), request.getSessionId()))
                || sameUser(existing, request.getActorUserId())) {
            return buildResponse(existing, target.submissionId(), target.collectionName(), FormSubmissionLockStatus.ALREADY_OWNED,
                    existing.getLockPurpose(), true, false);
        }
        return buildResponse(existing, target.submissionId(), target.collectionName(), FormSubmissionLockStatus.LOCKED_BY_OTHER,
                existing.getLockPurpose(), false, true);
    }

    @Override
    public void validateActiveLockOwnership(String submissionId, String collectionName, Long actorUserId, String lockToken) {
        Date now = Date.from(Instant.now());
        Query query = new Query(Criteria.where("submissionId").is(submissionId)
                .and("collectionName").is(collectionName)
                .and("lockedByUserId").is(actorUserId)
                .and("lockToken").is(lockToken)
                .and("expiresAt").gt(now));
        boolean valid = mongoTemplate.exists(query, FormSubmissionLock.class);
        if (!valid) {
            throw new IllegalStateException("A valid active form submission lock is required for this mutation.");
        }
    }

    private FormSubmissionLock acquireFreshLock(FormSubmissionLockAcquireRequest request, String canonicalCollectionName, Date now) {
        Query query = new Query(Criteria.where("submissionId").is(request.getSubmissionId())
                .and("collectionName").is(canonicalCollectionName)
                .and("expiresAt").lte(now));
        Update update = buildAcquireUpdate(request, canonicalCollectionName, now, UUID.randomUUID().toString());
        return mongoTemplate.findAndModify(query, update,
                FindAndModifyOptions.options().upsert(true).returnNew(true), FormSubmissionLock.class);
    }

    private FormSubmissionLock takeOverLock(FormSubmissionLock existing,
                                            FormSubmissionLockAcquireRequest request,
                                            String canonicalCollectionName,
                                            Date now) {
        Query query = new Query(Criteria.where("_id").is(existing.getId())
                .and("lockedByUserId").is(request.getActorUserId())
                .and("expiresAt").gt(now));
        Update update = buildAcquireUpdate(request, canonicalCollectionName, now, UUID.randomUUID().toString());
        FormSubmissionLock updated = mongoTemplate.findAndModify(query, update,
                FindAndModifyOptions.options().returnNew(true), FormSubmissionLock.class);
        return updated == null ? existing : updated;
    }

    private Update buildAcquireUpdate(FormSubmissionLockAcquireRequest request,
                                      String canonicalCollectionName,
                                      Date now,
                                      String newToken) {
        String sessionId = normalizedSessionId(request.getSessionId(), newToken);
        return new Update()
                .setOnInsert("submissionId", request.getSubmissionId())
                .setOnInsert("collectionName", canonicalCollectionName)
                .setOnInsert("createdAt", now)
                .set("lockToken", newToken)
                .set("sessionId", sessionId)
                .set("lockedByUserId", request.getActorUserId())
                .set("lockPurpose", request.getLockPurpose())
                .set("actorRoleIdsAtAcquire", sanitizeRoleIds(request.getActorRoleIds()))
                .set("expiresAt", expiryFrom(now))
                .set("updatedAt", now);
    }

    private String normalizedSessionId(String sessionId, String fallback) {
        if (sessionId == null || sessionId.isBlank()) {
            return fallback;
        }
        return sessionId;
    }

    private List<String> sanitizeRoleIds(List<String> actorRoleIds) {
        if (actorRoleIds == null || actorRoleIds.isEmpty()) {
            return null;
        }
        return actorRoleIds.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(roleId -> !roleId.isEmpty())
                .distinct()
                .toList();
    }

    private FormSubmissionLock findLock(String submissionId, String collectionName) {
        return formSubmissionLockRepository.findBySubmissionIdAndCollectionName(submissionId, collectionName).orElse(null);
    }

    private boolean isExpired(FormSubmissionLock lock, Date now) {
        return lock.getExpiresAt() == null || !lock.getExpiresAt().after(now);
    }

    private boolean ownedBy(FormSubmissionLock lock, Long actorUserId, String sessionId) {
        return lock != null
                && Objects.equals(lock.getLockedByUserId(), actorUserId)
                && Objects.equals(lock.getSessionId(), sessionId);
    }

    private boolean sameUser(FormSubmissionLock lock, Long actorUserId) {
        return lock != null && Objects.equals(lock.getLockedByUserId(), actorUserId);
    }

    private Date expiryFrom(Date now) {
        return Date.from(now.toInstant().plusSeconds(lockTtlSeconds));
    }

    private FormSubmissionLockResponse buildResponse(FormSubmissionLock lock,
                                                     String submissionId,
                                                     String collectionName,
                                                     FormSubmissionLockStatus status,
                                                     com.fps.svmes.enums.form.FormSubmissionLockPurpose purpose,
                                                     boolean ownedByCurrentCaller,
                                                     boolean readOnly) {
        FormSubmissionLockResponse response = new FormSubmissionLockResponse();
        response.setSubmissionId(submissionId);
        response.setCollectionName(collectionName);
        response.setStatus(status);
        response.setLockToken(lock == null ? null : lock.getLockToken());
        response.setSessionId(lock == null ? null : lock.getSessionId());
        response.setLockPurpose(lock == null ? purpose : lock.getLockPurpose());
        response.setExpiresAt(lock == null ? null : lock.getExpiresAt());
        response.setLockedByUserId(lock == null ? null : lock.getLockedByUserId());
        response.setOwnedByCurrentCaller(ownedByCurrentCaller);
        response.setReadOnly(readOnly);
        return response;
    }
}
