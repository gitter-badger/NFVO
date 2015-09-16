package org.project.openbaton.nfvo.vnfm_reg.tasks;

import org.project.openbaton.catalogue.mano.common.Event;
import org.project.openbaton.catalogue.mano.common.LifecycleEvent;
import org.project.openbaton.catalogue.mano.descriptor.VirtualDeploymentUnit;
import org.project.openbaton.catalogue.mano.record.Status;
import org.project.openbaton.catalogue.nfvo.Action;
import org.project.openbaton.catalogue.nfvo.CoreMessage;
import org.project.openbaton.catalogue.nfvo.messages.OrVnfmGenericMessage;
import org.project.openbaton.clients.exceptions.VimDriverException;
import org.project.openbaton.exceptions.VimException;
import org.project.openbaton.nfvo.core.interfaces.ResourceManagement;
import org.project.openbaton.nfvo.vnfm_reg.VnfmRegister;
import org.project.openbaton.nfvo.vnfm_reg.tasks.abstracts.AbstractTask;
import org.project.openbaton.vnfm.interfaces.sender.VnfmSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by lto on 06/08/15.
 */
@Service
@Scope("prototype")
public class AllocateresourcesTask extends AbstractTask {
    @Autowired
    private ResourceManagement resourceManagement;

    @Autowired
    @Qualifier("vnfmRegister")
    private VnfmRegister vnfmRegister;

    @Override
    protected void doWork() throws Exception {
        VnfmSender vnfmSender;
        vnfmSender = this.getVnfmSender(vnfmRegister.getVnfm(virtualNetworkFunctionRecord.getEndpoint()).getEndpointType());

        log.debug("NFVO: ALLOCATE_RESOURCES");
        log.debug("Verison is: " + virtualNetworkFunctionRecord.getHb_version());
        boolean error = false;
        for (VirtualDeploymentUnit vdu : virtualNetworkFunctionRecord.getVdu())
            try {
                List<String> extIds = resourceManagement.allocate(vdu, virtualNetworkFunctionRecord);
                log.debug("the returned ext id is: " + extIds);
            } catch (VimException e) {
                virtualNetworkFunctionRecord = vnfrRepository.save(virtualNetworkFunctionRecord);
                e.printStackTrace();
                log.error(e.getMessage());
                LifecycleEvent lifecycleEvent = new LifecycleEvent();
                lifecycleEvent.setEvent(Event.ERROR);
                virtualNetworkFunctionRecord.getLifecycle_event_history().add(lifecycleEvent);
                vnfmSender.sendCommand(new OrVnfmGenericMessage(virtualNetworkFunctionRecord, Action.ERROR), getTempDestination());
                error = true;
            } catch (VimDriverException e) {
                virtualNetworkFunctionRecord = vnfrRepository.save(virtualNetworkFunctionRecord);
                e.printStackTrace();
                log.error(e.getMessage());
                LifecycleEvent lifecycleEvent = new LifecycleEvent();
                lifecycleEvent.setEvent(Event.ERROR);
                virtualNetworkFunctionRecord.getLifecycle_event_history().add(lifecycleEvent);
                virtualNetworkFunctionRecord.setStatus(Status.ERROR);
                virtualNetworkFunctionRecord = vnfrRepository.save(virtualNetworkFunctionRecord);
                vnfmSender.sendCommand(new OrVnfmGenericMessage(virtualNetworkFunctionRecord,Action.ERROR), getTempDestination());
                error = true;
            }

        for (LifecycleEvent event : virtualNetworkFunctionRecord.getLifecycle_event()) {
            if (event.getEvent().ordinal() == Event.ALLOCATE.ordinal()) {
                virtualNetworkFunctionRecord.getLifecycle_event_history().add(event);
                break;
            }
        }
        Thread.sleep(1000 * ((int) (Math.random() * 3 + 1)));
        if (!error) {

            for (VirtualDeploymentUnit virtualDeploymentUnit : virtualNetworkFunctionRecord.getVdu()) {
                log.debug(">---< The unit is: " + virtualDeploymentUnit);
            }
            OrVnfmGenericMessage orVnfmGenericMessage = new OrVnfmGenericMessage(virtualNetworkFunctionRecord,Action.ALLOCATE_RESOURCES);
            log.debug("SENDING ALLOCATE RESOURCES on temp queue:" + getTempDestination());
            vnfmSender.sendCommand(orVnfmGenericMessage, getTempDestination());
        }
    }

    @Override
    public boolean isAsync() {
        return true;
    }
}
