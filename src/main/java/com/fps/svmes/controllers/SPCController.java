package com.fps.svmes.controllers;

import com.fps.svmes.dto.dtos.spc.SPCDTO;
import com.fps.svmes.dto.requests.SPCRequest;
import com.fps.svmes.dto.responses.ResponseResult;
import com.fps.svmes.services.SPCService;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.validation.Valid;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/spc")
public class SPCController {

    private final SPCService spcService;

    @Autowired
    public SPCController(SPCService spcService) {
        this.spcService = spcService;
    }

    @GetMapping("")
    public ResponseResult<List<SPCDTO>> getSPCData(
            @Valid @RequestBody SPCRequest request
            ) throws IllegalArgumentException {
        try {
            List<SPCDTO> results = spcService.getSPCData(request);
            return ResponseResult.success(results);
        } catch (Exception e) {
            return ResponseResult.fail("Error fetching SPC data: " + e);
        }
    }
}
