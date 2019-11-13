package c8y.example;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.math.RandomUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cumulocity.microservice.autoconfigure.MicroserviceApplication;
import com.cumulocity.microservice.subscription.service.MicroserviceSubscriptionsService;
import com.cumulocity.model.ID;
import com.cumulocity.model.idtype.GId;
import com.cumulocity.rest.representation.identity.ExternalIDRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjects;
import com.cumulocity.rest.representation.measurement.MeasurementRepresentation;
import com.cumulocity.sdk.client.Param;
import com.cumulocity.sdk.client.QueryParam;
import com.cumulocity.sdk.client.SDKException;
import com.cumulocity.sdk.client.identity.IdentityApi;
import com.cumulocity.sdk.client.inventory.InventoryApi;
import com.cumulocity.sdk.client.measurement.MeasurementApi;
import com.cumulocity.sdk.client.measurement.MeasurementCollection;
import com.cumulocity.sdk.client.measurement.MeasurementFilter;

import c8y.IsDevice;


@MicroserviceApplication
@RestController
public class App{
		
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @RequestMapping("hello")
    public String greeting(@RequestParam(value = "name", defaultValue = "world") String name) {
        return "hello " + name + "!";
    }

    // You need the inventory API to handle managed objects e.g. creation. You will find this class within the C8Y java client library.
    private final InventoryApi inventoryApi;
    
    // you need the identity API to handle the external ID e.g. IMEI of a managed object. You will find this class within the C8Y java client library.
    private final IdentityApi identityApi;
    
    // you need the measurement API to handle measurements. You will find this class within the C8Y java client library.
    private final MeasurementApi measurementApi;
    
    // Microservice subscription
    private final MicroserviceSubscriptionsService subscriptionService;
    
    
    @Autowired
    public App( InventoryApi inventoryApi, 
    			IdentityApi identityApi, 
    			MicroserviceSubscriptionsService subscriptionService,
    			MeasurementApi measurementApi) {
        this.inventoryApi = inventoryApi;
        this.identityApi = identityApi;
        this.subscriptionService = subscriptionService;
        this.measurementApi = measurementApi;
    }
    
    // Create every x sec a new measurement
    @Scheduled(initialDelay=10000, fixedDelay=5000)
    public void startThread() {
    	subscriptionService.runForEachTenant(new Runnable() {
			@Override
			public void run() {
		    	ManagedObjectRepresentation managedObjectRepresentation = resolveManagedObject();
		    	try {
		    		createTemperatureMeasurement(managedObjectRepresentation);		    		
				} catch (Exception e) {
					e.printStackTrace();
				} 
			}
		});
    }
        
    // Create a new managed object + external ID (if not existing)  
    private ManagedObjectRepresentation resolveManagedObject() {
       	
    	try {
        	// check if managed object is existing. create a new one if the managed object is not existing
    		ExternalIDRepresentation externalIDRepresentation = identityApi.getExternalId(new ID("c8y_Serial", "Microservice-Part6_externalId"));
			return externalIDRepresentation.getManagedObject();    	    	

    	} catch(SDKException e) {
    		    		
    		// create a new managed object
			ManagedObjectRepresentation newManagedObject = new ManagedObjectRepresentation();
	    	newManagedObject.setName("Microservice-Part6");
	    	newManagedObject.setType("Microservice-Part6");
	    	newManagedObject.set(new IsDevice());	    	
	    	ManagedObjectRepresentation createdManagedObject = inventoryApi.create(newManagedObject);
	    	
	    	// create an external id and add the external id to an existing managed object
	    	ExternalIDRepresentation externalIDRepresentation = new ExternalIDRepresentation();
	    	// Definition of the external id
	    	externalIDRepresentation.setExternalId("Microservice-Part6_externalId");
	    	// Assign the external id to an existing managed object
	    	externalIDRepresentation.setManagedObject(createdManagedObject);
	    	// Definition of the serial
	    	externalIDRepresentation.setType("c8y_Serial");
	    	// Creation of the external id
	    	identityApi.create(externalIDRepresentation);
	    	
	    	return createdManagedObject;
    	}
    }
    
    
    // Create a new custom measurement (CustomTemperatureMeasurement.java) 
	public void createTemperatureMeasurement(ManagedObjectRepresentation managedObjectRepresentation) {
		
		// Create a new custom temperature measurement
		CustomTemperatureMeasurement customTemperatureMeasurement = new CustomTemperatureMeasurement();
		// Set the temperature random value
		customTemperatureMeasurement.setTemperature(BigDecimal.valueOf(RandomUtils.nextInt(100)));
		
		// Create a new measurement representation
		MeasurementRepresentation measurementRepresentation = new MeasurementRepresentation();
		// Define the managed object where you would like to send the measurements
		measurementRepresentation.setSource(ManagedObjects.asManagedObject(GId.asGId(managedObjectRepresentation.getId())));
		// Set the generation time of the measurement
		measurementRepresentation.setDateTime(new DateTime());
		// Set the type of the planned measurement e.g. temperature
		measurementRepresentation.setType("c8y_CustomTemperatureMeasurement");
		// Set the temperature measurement you defined before
		measurementRepresentation.set(customTemperatureMeasurement);
		
		// Create the measurement
		measurementApi.create(measurementRepresentation);
	}
	
	
	// Read the latest custom measurement
	@RequestMapping("latestMeasurement")
	public String getCustomMeasurement() {
		
		// Managed object representation will give you access to the ID of the mnaged object
		ManagedObjectRepresentation managedObjectRepresentation = resolveManagedObject();
		
		// getMeasurementsByFilter will NOT return a list of measurements!.
		// You will give you access to the collection object.
		// We will reduce the number of measurements by using filter criteria e.g. byDate or BySource etc.
		// It is not possible to sort the measurements because the needed filter creteria is not existing
		MeasurementCollection measurementCollection = measurementApi.getMeasurementsByFilter(new MeasurementFilter().byType("c8y_CustomTemperatureMeasurement")
																	  												.byDate(new Date(0), new Date())
																	  												.byValueFragmentTypeAndSeries("c8y_example_CustomTemperatureMeasurement", "temperatureOutside")
																	  												.bySource(managedObjectRepresentation.getId()));
		
		// Because sorting was not possible we will send a 'revert = true' REST request to get a sorted list. 1 will reduce the number of results.		
		List<MeasurementRepresentation> measurementList = measurementCollection.get(1, new QueryParam(new Param() {
			@Override
			public String getName() {
				return "revert";
			}
		}, "true")).getMeasurements();
		
		// Return the latest measurement
		if(measurementList.size()>0) {
			return measurementList.get(0).toJSON();
		}
		
		return "no value existing";
	}
    
}