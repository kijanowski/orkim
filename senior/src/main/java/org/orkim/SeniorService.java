package org.orkim;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

@ApplicationPath("/senior")
public class SeniorService extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> set =  new HashSet<>();
        set.add(Senior.class);
        return set;
    }

}
