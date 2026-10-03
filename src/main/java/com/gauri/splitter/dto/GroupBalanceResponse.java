package com.gauri.splitter.dto;

import java.util.List;

public record GroupBalanceResponse(List<BalanceResponse> balances,
                                   List<TransferResponse> settlements) {}