package comiam.factoryapp.factory.factory;

import comiam.factoryapp.factory.dealer.Dealer;
import comiam.factoryapp.factory.events.EventHandler;
import comiam.factoryapp.factory.events.EventManager;
import comiam.factoryapp.factory.producer.ProducerSection;
import comiam.factoryapp.factory.store.*;
import comiam.factoryapp.factory.supplier.AccessorySupplier;
import comiam.factoryapp.factory.supplier.BodyworkSupplier;
import comiam.factoryapp.factory.supplier.EngineSupplier;
import comiam.factoryapp.log.Log;
import comiam.factoryapp.time.Timer;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class Factory
{
    private boolean initialized = false;
    private EventManager eventManager;
    private ArrayList<Thread> threadPool;
    private CarStore carStore;
    private AccessoryStore accessoryStore;
    private BodyworkStore bodyworkStore;
    private EngineStore engineStore;
    private ProducerSection producerSection;
    private CarStoreController carStoreController;

    private final int accessorySupplierCount;
    private final int producerCount;
    private final int dealerCount;
    private int supplierDelay;
    private int producerDelay;
    private int dealerDelay;
    private final int accessoryStoreLimit;
    private final int engineStoreLimit;
    private final int bodyworkStoreLimit;
    private final int carStoreLimit;

    /**
     * If any delay equals -1, when this delay sets randomly from 0,2 to 1.
     * @param accessorySupplierCount - count of suppliers for this factory.
     * @param producerCount - count of producers for this factory.
     * @param dealerCount - count of dealers for this factory.
     * @param supplierDelay - delay in milliseconds with which suppliers deliver components.
     * @param producerDelay - delay in milliseconds with which producers make cars.
     * @param dealerDelay - delay in milliseconds with which dealers buy cars.
     * @param accessoryStoreLimit - limit of components for accessory store
     * @param engineStoreLimit - limit of components for engine store
     * @param bodyworkStoreLimit - limit of components for bodywork store
     * @param carStoreLimit - limit of components for car store
     */
    public Factory(int accessorySupplierCount, int producerCount, int dealerCount, int supplierDelay, int producerDelay, int dealerDelay,
                   int accessoryStoreLimit, int engineStoreLimit, int bodyworkStoreLimit, int carStoreLimit)
    {
        eventManager = new EventManager();

        if(supplierDelay == -1)
            supplierDelay = (int)(randomizeDelay() * 1000);

        if(producerDelay == -1)
            supplierDelay = (int)(randomizeDelay() * 1000);

        if(dealerDelay == -1)
            supplierDelay = (int)(randomizeDelay() * 1000);

        this.accessorySupplierCount = accessorySupplierCount;
        this.dealerCount = dealerCount;
        this.producerCount = producerCount;
        this.supplierDelay = supplierDelay;
        this.producerDelay = producerDelay;
        this.dealerDelay = dealerDelay;
        this.accessoryStoreLimit = accessoryStoreLimit;
        this.engineStoreLimit = engineStoreLimit;
        this.bodyworkStoreLimit = bodyworkStoreLimit;
        this.carStoreLimit = carStoreLimit;
    }

    /**
     * Destroy all threads in factory, factory will stop. All data will be deleted.
     */
    public synchronized void destroy(boolean destroyEventListeners)
    {
        if(!initialized)
            return;

        initialized = false;

        carStore = null;
        accessoryStore = null;
        bodyworkStore = null;
        engineStore = null;

        stopThreads();
        threadPool.clear();
        carStoreController = null;
        producerSection = null;
        threadPool = null;

        if(destroyEventListeners)
            eventManager = null;
        System.gc();
        Timer.stop();
    }

    /**
     * Init all factory processes: Dealers, Producers and suppliers.
     */
    public synchronized void init()
    {
        if(initialized)
            return;

        producerSection = new ProducerSection(this);
        carStore = new CarStore(this, carStoreLimit);
        accessoryStore = new AccessoryStore(this, accessoryStoreLimit);
        bodyworkStore = new BodyworkStore(this, bodyworkStoreLimit);
        engineStore = new EngineStore(this, engineStoreLimit);
        carStoreController = new CarStoreController(this);
        threadPool = new ArrayList<>();

        for(int i = 0; i < accessorySupplierCount; i++)
            threadPool.add(new AccessorySupplier(this));

        threadPool.add(new EngineSupplier(this));
        threadPool.add(new BodyworkSupplier(this));

        for(int i = 0; i < dealerCount; i++)
            threadPool.add(new Dealer(this, i));


        initialized = true;
        Timer.start();
        startThreads();
    }

    public synchronized void restart()
    {
        if(!initialized)
            return;

        destroy(false);
        init();
    }

    private synchronized void stopThreads()
    {
        producerSection.destroy();
        carStoreController.interrupt();

        for(var thread : threadPool)
            thread.interrupt();
    }

    private void startThreads()
    {
        carStoreController.start();
        producerSection.start();
        for(var thread : threadPool)
            thread.start();
    }

    public synchronized boolean isInitialized()
    {
        return initialized;
    }

    public synchronized CarStore getCarStore()
    {
        return carStore;
    }

    public synchronized AccessoryStore getAccessoryStore()
    {
        return accessoryStore;
    }

    public synchronized BodyworkStore getBodyworkStore()
    {
        return bodyworkStore;
    }

    public synchronized EngineStore getEngineStore()
    {
        return engineStore;
    }

    public synchronized ProducerSection getProducerSection()
    {
        return producerSection;
    }

    public synchronized EventManager getEventManager()
    {
        return eventManager;
    }

    public synchronized int getAccessorySupplierCount()
    {
        return accessorySupplierCount;
    }

    public synchronized int getProducerCount()
    {
        return producerCount;
    }

    public synchronized int getDealerCount()
    {
        return dealerCount;
    }

    public synchronized int getSupplierDelay()
    {
        return supplierDelay;
    }

    public synchronized int getProducerDelay()
    {
        return producerDelay;
    }

    public synchronized int getDealerDelay()
    {
        return dealerDelay;
    }

    public synchronized void setSupplierDelay(int supplierDelay)
    {
        if(supplierDelay == -1)
            this.supplierDelay = (int)(randomizeDelay() * 1000);
        else
            this.supplierDelay = supplierDelay;
    }

    public synchronized void setProducerDelay(int producerDelay)
    {
        if(producerDelay == -1)
            this.producerDelay = (int)(randomizeDelay() * 1000);
        else
            this.producerDelay = producerDelay;
    }

    public synchronized void setDealerDelay(int dealerDelay)
    {
        if(dealerDelay == -1)
            this.dealerDelay = (int)(randomizeDelay() * 1000);
        else
            this.dealerDelay = dealerDelay;
    }

    public void enableLogging() throws Exception
    {
        Log.init();
        Log.enableInfoLogging();
    }

    /**
     * @return random value from 0 to 2
     */
    public static double randomizeDelay()
    {
        return 2 * ThreadLocalRandom.current().nextDouble();
    }

    //EVENT SECTION

    public void setOnEngineDelivered(EventHandler handler)
    {
        if(handler == null)
            return;
        eventManager.setEventHandler(EventManager.ENGINE_DELIVERED_EVENT, handler);
    }

    public void setOnAccessoryDelivered(EventHandler handler)
    {
        if(handler == null)
            return;
        eventManager.setEventHandler(EventManager.ACCESSORY_DELIVERED_EVENT, handler);
    }

    public void setOnBodyworkDelivered(EventHandler handler)
    {
        if(handler == null)
            return;
        eventManager.setEventHandler(EventManager.BODYWORK_DELIVERED_EVENT, handler);
    }

    public void setOnCareMade(EventHandler handler)
    {
        if(handler == null)
            return;
        eventManager.setEventHandler(EventManager.CAR_MADE_EVENT, handler);
    }

    public void setOnCarDelivered(EventHandler handler)
    {
        if(handler == null)
            return;
        eventManager.setEventHandler(EventManager.CAR_SUPPLIED_TO_STORE_EVENT, handler);
    }

    public void setOnComponentSendFromStore(EventHandler handler)
    {
        if(handler == null)
            return;
        eventManager.setEventHandler(EventManager.COMPONENT_SEND_FROM_STORE, handler);
    }

    public void setOnCarSend(EventHandler handler)
    {
        if(handler == null)
            return;
        eventManager.setEventHandler(EventManager.CAR_SEND_EVENT, handler);
    }

    public void setOnProducerStartJob(EventHandler handler)
    {
        if(handler == null)
            return;
        eventManager.setEventHandler(EventManager.PRODUCER_STARTED_DO_JOB_EVENT, handler);
    }

    public void setOnProducerDidJob(EventHandler handler)
    {
        if(handler == null)
            return;
        eventManager.setEventHandler(EventManager.PRODUCER_DID_JOB_EVENT, handler);
    }
}