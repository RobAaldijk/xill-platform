package nl.xillio.xill.webservice.services;

import nl.xillio.xill.webservice.exceptions.XillAllocateWorkerException;
import nl.xillio.xill.webservice.exceptions.XillInvalidStateException;
import nl.xillio.xill.webservice.exceptions.XillNotFoundException;
import nl.xillio.xill.webservice.types.XWID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * This class represents the service for the web controller.
 */
@Service
public class XillWebService {

    private final XillWorkerPoolManagerService workerPoolManagerService;

    @Autowired
    public XillWebService(XillWorkerPoolManagerServiceImpl workerPoolManagerService) {
        this.workerPoolManagerService = workerPoolManagerService;
    }

    /**
     * Allocate a worker for a specific robot if a space is available.
     *
     * @param robotFQN the robot identificator that should be connected to a worker
     * @return the identifier for the worker
     */
    public XWID allocateWorker(String robotFQN) throws XillAllocateWorkerException, XillNotFoundException {
        return workerPoolManagerService.getDefaultWorkerPool().allocateWorker(robotFQN).getId();
    }

    /**
     * Release the worker with a specific identifier.
     *
     * @param id the identifier of the worker
     */
    public void releaseWorker(XWID id) throws XillNotFoundException, XillInvalidStateException {
        workerPoolManagerService.getDefaultWorkerPool().releaseWorker(id);
    }

    /**
     * Release all workers in all worker pools
     */
    public void releaseAllWorkers() {
        workerPoolManagerService.getAllWorkerPools().forEach(wp -> {
            wp.releaseAllWorkers();
        });
    }

    /**
     * Run existing worker (i.e. run robot associated with the worker)
     *
     * @param id The XillWorker id.
     * @param parameters The parameters used for the associated robot run.
     * @return The result from robot run.
     * @throws XillNotFoundException if the worker does not exist.
     * @throws XillInvalidStateException if the worker is not in the required state.
     */
    public Object runWorker(XWID id, final Map<String, Object> parameters) throws XillNotFoundException, XillInvalidStateException {
        return workerPoolManagerService.getDefaultWorkerPool().findWorker(id).run(parameters);
    }

    /**
     * Interrupt and stop the running worker (robot).
     *
     * @param id he XillWorker id.
     * @throws XillNotFoundException if the worker does not exist.
     */
    public void abortWorker(XWID id) throws XillNotFoundException {
        workerPoolManagerService.getDefaultWorkerPool().findWorker(id).abort();
    }
}
