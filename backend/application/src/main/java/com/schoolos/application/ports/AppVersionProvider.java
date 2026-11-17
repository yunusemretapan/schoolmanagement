package com.schoolos.application.ports;

/** Port: the running application's build version. Implemented outside the application layer. */
public interface AppVersionProvider {

    String version();
}
