package li.cil.oc.server.machine;

import li.cil.oc.api.machine.Callback;

import java.lang.annotation.Annotation;

// Java class to avoid those stupid warnings.
@SuppressWarnings("ClassExplicitlyAnnotation")
public class PeripheralAnnotation implements Callback {
    private final String name;
    private final boolean isDirect;

    public PeripheralAnnotation(final String name, boolean isDirect) {
        this.name = name;
        this.isDirect = isDirect;
    }

    @Override
    public String value() {
        return name;
    }

    @Override
    public boolean direct() {
        return isDirect;
    }

    @Override
    public int limit() {
        return 100;
    }

    @Override
    public String doc() {
        return "";
    }

    @Override
    public boolean getter() {
        return false;
    }

    @Override
    public boolean setter() {
        return false;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return Callback.class;
    }
}
