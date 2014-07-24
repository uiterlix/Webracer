package net.slintes.webracer.track.one;

import net.slintes.webracer.track.Track;
import net.slintes.webracer.track.one.impl.TrackOne;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 *
 */
public class TrackActivator implements BundleActivator {

    private ServiceRegistration<?> registration;

    @Override
    public void start(BundleContext context) throws Exception {
        Track track = new TrackOne();
        registration = context.registerService(Track.class.getName(), track, null);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        registration.unregister();
    }
}
