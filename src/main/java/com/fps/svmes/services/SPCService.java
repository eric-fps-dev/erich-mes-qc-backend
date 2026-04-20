package com.fps.svmes.services;


import com.fps.svmes.dto.dtos.spc.SPCDTO;
import com.fps.svmes.dto.requests.SPCRequest;
import org.apache.coyote.BadRequestException;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;

public interface SPCService {

    List<SPCDTO> getSPCData(SPCRequest request) throws IllegalArgumentException;
}
