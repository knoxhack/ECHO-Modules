package com.knoxhack.echolens.api;

public interface ILensInspectionService {
    ILensInspectionService NOOP = context -> LensReport.empty();

    LensReport inspect(LensContext context);
}
