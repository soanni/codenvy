package com.codenvy.ldap.sync;

import com.codenvy.api.permission.server.PermissionsManager;
import com.codenvy.api.permission.server.SystemDomain;
import com.codenvy.api.permission.server.model.impl.SystemPermissionsImpl;
import com.google.inject.Inject;
import com.google.inject.Provider;

import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.core.notification.EventSubscriber;
import org.eclipse.che.api.user.server.event.AfterUserPersistedEvent;

import javax.persistence.EntityManager;
import java.util.List;

public class FakeListener implements EventSubscriber<AfterUserPersistedEvent> {

    @Inject
    private PermissionsManager permManager;

    @Inject
    private EventService eventService;

    @Inject
    private Provider<EntityManager> emProvider;

    @Override
    public void onEvent(AfterUserPersistedEvent event) {
        try {
            final List<String> actions = permManager.getDomain(SystemDomain.DOMAIN_ID).getAllowedActions();
            final SystemPermissionsImpl perm = new SystemPermissionsImpl(event.getUser().getId(), actions);
            permManager.storePermission(perm);
        } catch (Exception x) {
            System.out.println(x.getLocalizedMessage());
        }
    }

    public void subscribe() {
        eventService.subscribe(this);
    }
}
