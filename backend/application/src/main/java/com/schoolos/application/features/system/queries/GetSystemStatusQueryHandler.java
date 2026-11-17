package com.schoolos.application.features.system.queries;

import com.schoolos.application.features.system.constants.SystemStatus;
import com.schoolos.application.features.system.dtos.SystemStatusResponse;
import com.schoolos.application.ports.AppVersionProvider;
import com.schoolos.core.application.mediator.RequestHandler;

public final class GetSystemStatusQueryHandler implements RequestHandler<GetSystemStatusQuery, SystemStatusResponse> {

    private final AppVersionProvider versionProvider;

    public GetSystemStatusQueryHandler(AppVersionProvider versionProvider) {
        this.versionProvider = versionProvider;
    }

    @Override
    public Class<GetSystemStatusQuery> requestType() {
        return GetSystemStatusQuery.class;
    }

    @Override
    public SystemStatusResponse handle(GetSystemStatusQuery request) {
        return new SystemStatusResponse(SystemStatus.UP, versionProvider.version());
    }
}
