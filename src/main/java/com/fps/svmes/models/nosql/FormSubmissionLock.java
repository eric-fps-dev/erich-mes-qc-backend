package com.fps.svmes.models.nosql;

import com.fps.svmes.enums.form.FormSubmissionLockPurpose;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;
import java.util.List;

@Document(collection = "qc-form-submission-lock")
@CompoundIndexes({
        @CompoundIndex(name = "ux_submission_collection", def = "{'submissionId': 1, 'collectionName': 1}", unique = true)
})
@Data
public class FormSubmissionLock {
    @Id
    private String id;

    @Field("submissionId")
    private String submissionId;

    @Field("collectionName")
    private String collectionName;

    @Field("lockToken")
    private String lockToken;

    @Field("sessionId")
    private String sessionId;

    @Field("lockedByUserId")
    private Long lockedByUserId;

    @Field("lockPurpose")
    private FormSubmissionLockPurpose lockPurpose;

    @Field("actorRoleIdsAtAcquire")
    private List<String> actorRoleIdsAtAcquire;

    @Indexed(name = "idx_submission_lock_expires_at", expireAfterSeconds = 0)
    @Field("expiresAt")
    private Date expiresAt;

    @Field("createdAt")
    private Date createdAt;

    @Field("updatedAt")
    private Date updatedAt;
}
