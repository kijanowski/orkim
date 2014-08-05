package org.orkim;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

@ApplicationPath("/junior")
public class JuniorService extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> set =  new HashSet<>();
        set.add(Junior.class);
        return set;
    }

}