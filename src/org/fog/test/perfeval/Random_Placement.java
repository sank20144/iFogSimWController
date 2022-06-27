/**
 * 
 */
package org.fog.test.perfeval;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.entities.Actuator;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.entities.Sensor;
import org.fog.entities.Tuple;
import org.fog.placement.Controller;
import org.fog.placement.ModuleMapping;
import org.fog.placement.ModulePlacementEdgewards;
import org.fog.placement.ModulePlacementMapping;
import org.fog.placement.ModulePlacementOnlyCloud;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.utils.TimeKeeper;
import org.fog.utils.distribution.DeterministicDistribution;





/**
 * @author This class will implement the work of Random_Placement
 *         hypothesis
 */
public class Random_Placement {
	
	public static Controller controller = null;
	// this will maintain the list of all the fog devices
	static List<FogDevice> fogDevices = new ArrayList<FogDevice>();
	// this will contain the list of all the sensors
	static List<Sensor> sensors = new ArrayList<Sensor>();
	// this will contain the list of all the actuators
	static List<Actuator> actuators = new ArrayList<Actuator>();
	// this is going to create one router
	static int numOfAreas = 2;
	// this is going to create the number of edge devices
	static int numOfCamerasPerArea = 4;
	// this variable will tell the system that all the load is to be transfered to
	// the cloud node.
	private static boolean CLOUD = false;
	private static boolean CAPACITY = false; // Capacity placement defaults to power utilization
	private static boolean EDGEWARDS = false;
	
	// code added by anupinder from ifogsim tutorial
	static Map<String, Integer> getIdByName = new HashMap<String, Integer>();

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		// just a console message that is being printed for indication
		Log.printLine("Starting Random_Placement Scenario...");

		try {
			Log.disable();// if commented will print all the cloudsim messages as well
			
			int num_user = 1; // number of cloud users
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false; // mean trace events
			CloudSim.init(num_user, calendar, trace_flag);
			String appId = "Random_PlacementSimulation"; // identifier of the application
			FogBroker broker = new FogBroker("broker");
			
			Application application = createApplication(appId, broker.getId());

			application.setUserId(broker.getId());
			
			// now i need to create a set of Fog Devices
			//Level 1 - Cloud
			//Level 2 - Three Fog devices
			//LEvel 3 - One gateway
			//level 4 - multiple sensors, multiple actuators
			createTopology(broker.getId(), appId);
			
			
			
			ModuleMapping moduleMapping = ModuleMapping.createModuleMapping(); // initializing a module mapping
			
			//now for each node on the topology.. we will match the names and then 
			for(FogDevice device : fogDevices){
				if(device.getName().startsWith("m")){ // names of all Smart Cameras start with 'm' 
					moduleMapping.addModuleToDevice("motion_detector", device.getName());  // fixing 1 instance of the Motion Detector module to each Smart Camera
				}
				if(device.getName().startsWith("g")){ // names of all gateway start with 'g' 
					moduleMapping.addModuleToDevice("masterController", device.getName());  // fixing 1 instance of the Motion Detector module to each Smart Camera
				}
				if(device.getName().startsWith("f")){ // names of all fog nodes start with 'f' 
					moduleMapping.addModuleToDevice("worker_node", device.getName());  // fixing 1 instance of the Motion Detector module to each Smart Camera
				}
			}
			//user interface is hardcodedly mapped to cloud device
			moduleMapping.addModuleToDevice("user_interface", "cloud"); // fixing instances of User Interface module in the Cloud
			if(CLOUD){
				// if the mode of deployment is cloud-based
				moduleMapping.addModuleToDevice("masterController", "cloud"); // placing all instances of Object Detector module in the Cloud
				moduleMapping.addModuleToDevice("worker_node", "cloud"); // placing all instances of Object Tracker module in the Cloud
			}
			// this is going to be the heart of the fog computing simulation and this is going to initialize and setup the basic placements 
			controller = new Controller("master-controller", fogDevices, sensors, actuators);
			//sFogUtils.fogDevices = fogDevices;
			
			if (CLOUD) {
				controller.submitApplication(application, (new ModulePlacementMapping(fogDevices, application, moduleMapping)));
			} else {
				controller.submitApplication(application, (new ModulePlacementEdgewards(fogDevices, sensors, actuators, application, moduleMapping))); // we are using edgewords Placement
			}
			
			TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());
			
			CloudSim.startSimulation();

			CloudSim.stopSimulation();

			Log.enable();
			Log.printLine("Random_Placement Scenario is completed...");
		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("Unwanted errors happen");
		}
	}
	
	// this method approaches to create the topology from top to bottom
	private static void createTopology(int userId, String appId) {
		// step 1 Cloud device is created 
		FogDevice cloud = createFogDevice("cloud", 44800, 40000, 100, 10000, 0, 0.01, 16*103, 16*83.25);// inturn calls the PowerdataCenter
		cloud.setParentId(-1);
		fogDevices.add(cloud);
		
		// Step 2- three fog nodes of same capacity are required to be created
		//step 2a- Create the node devices
		for(int i=0;i<numOfAreas;i++){
			addArea(i+"", userId, appId, cloud.getId());						  
		}
		
		//Propagates placement strategy
		//FogUtils.CAPACITY = CAPACITY;
		//FogUtils.CLOUD = CLOUD;
		//FogUtils.EDGEWARDS = EDGEWARDS;
	}
	
	private static void addArea(String id, int userId, String appId, int parentId){
		
		long mips = 2800;
		int ram = 4000; 
		long upBw = 10000; 
		long downBw = 10000; 
		double ratePerMips = 0; 
		double busyPower = 107.339; 
		double idlePower = 83.4333;
		
		FogDevice fog1 = createFogDevice("f-fog1", mips, ram, upBw, downBw, 1, ratePerMips, busyPower, idlePower);
		//FogDevice fog2 = createFogDevice("f-fog2", mips, ram, upBw, downBw, 1, ratePerMips, busyPower, idlePower);
		//FogDevice fog3 = createFogDevice("f-fog3", mips, ram, upBw, downBw, 1, ratePerMips, busyPower, idlePower);
		//step2b- Set their parent ID to Cloud
		fog1.setParentId(parentId);
		//fog2.setParentId(parentId);
		//fog3.setParentId(parentId);
		//step2c- Set up their network latency
		fog1.setUplinkLatency(100);
		//fog2.setUplinkLatency(100);
		//fog3.setUplinkLatency(100);
		
		/*fog1.setxCoordinate(10);fog1.setyCoordinate(15);
		fog2.setxCoordinate(10);fog2.setyCoordinate(15);
		fog3.setxCoordinate(10);fog3.setyCoordinate(15);*/
		
		// step2d- finally add them to the list
		
		
		//step 3 - create a gateway
		FogDevice gateway = createFogDevice("g-gateway", 800, 400, 10000, 10000, 2, 0.0, 107.339, 83.4333);
		gateway.setParentId(fog1.getId());
		
		gateway.setUplinkLatency(10);
		
		fogDevices.add(fog1);
		//fogDevices.add(fog2);
		//fogDevices.add(fog3);
		fogDevices.add(gateway);
		
		//Step 4 - finally the end nodes are going to be created for the purpose of collecting the data
		//right now only creating one single node to test the system, for more just a loop is required
		
		for(int i=0;i<numOfCamerasPerArea;i++){
			String mobileId = id+"-"+i;
			FogDevice camera = addCamera(mobileId, userId, appId, fog1.getId()); // adding a smart camera to the physical topology. Smart cameras have been modeled as fog devices as well.
			camera.setUplinkLatency(2); // latency of connection between camera and router is 2 ms
			fogDevices.add(camera);
		}
	}
	
	private static FogDevice addCamera(String id, int userId, String appId, int parentId){
		FogDevice camera = createFogDevice("m-"+id, 500, 1000, 10000, 10000, 3, 0, 87.53, 82.44);
		camera.setParentId(parentId);
		Sensor sensor = new Sensor("s-"+id, "CAMERA", userId, appId, new DeterministicDistribution(5)); // inter-transmission time of camera (sensor) follows a deterministic distribution
		sensor.setGatewayDeviceId(camera.getId());
		sensor.setLatency(1.0);  // latency of connection between camera (sensor) and the parent Smart Camera is 1 ms
		sensors.add(sensor);
		Actuator ptz = new Actuator("ptz-"+id, userId, appId, "PTZ_CONTROL");
		ptz.setGatewayDeviceId(camera.getId());
		ptz.setLatency(1.0);  // latency of connection between PTZ Control and the parent Smart Camera is 1 ms
		actuators.add(ptz);
		return camera;
	}	
		
	private static FogDevice createFogDevice(String nodeName, long mips,
			int ram, long upBw, long downBw, int level, double ratePerMips, double busyPower, double idlePower) {
		
		List<Pe> peList = new ArrayList<Pe>();

		// 3. Create PEs and add these into a list.
		peList.add(new Pe(0, new PeProvisionerOverbooking(mips))); // need to store Pe id and MIPS Rating

		int hostId = FogUtils.generateEntityId();
		long storage = 1000000; // host storage
		int bw = 10000;

		PowerHost host = new PowerHost(
				hostId,
				new RamProvisionerSimple(ram),
				new BwProvisionerOverbooking(bw),
				storage,
				peList,
				new StreamOperatorScheduler(peList),
				new FogLinearPowerModel(busyPower, idlePower)
			);

		List<Host> hostList = new ArrayList<Host>();
		hostList.add(host);

		String arch = "x86"; // system architecture
		String os = "Linux"; // operating system
		String vmm = "Xen";
		double time_zone = 10.0; // time zone this resource located
		double cost = 3.0; // the cost of using processing in this resource
		double costPerMem = 0.05; // the cost of using memory in this resource
		double costPerStorage = 0.001; // the cost of using storage in this
										// resource
		double costPerBw = 0.0; // the cost of using bw in this resource
		LinkedList<Storage> storageList = new LinkedList<Storage>(); // we are not adding SAN
													// devices by now

		FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(
				arch, os, vmm, host, time_zone, cost, costPerMem,
				costPerStorage, costPerBw);

		FogDevice fogdevice = null;
		try {
			fogdevice = new FogDevice(nodeName, characteristics, 
					new AppModuleAllocationPolicy(hostList), storageList, 10, upBw, downBw, 0, ratePerMips);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		fogdevice.setLevel(level);
		return fogdevice;
	}
	
	//used to generate some randomness in the geolocation co-ordinates for clustering purpose
	private static double getValue(double min) {
		Random rn = new Random();
		return rn.nextDouble()*10 + min;
		}

	@SuppressWarnings({"serial" })
	private static Application createApplication(String appId, int userId){
		Application application = Application.createApplication(appId, userId);
		/*
		 * Adding modules (vertices) to the application model (directed graph)
		 */
		application.addAppModule("motion_detector", 10);// host on camera
		application.addAppModule("masterController", 10);// host on gateway
		application.addAppModule("worker_node", 100);// host on processing node
		application.addAppModule("user_interface", 10);// host on cloud
		
		
		/*	
		 * Connecting the application modules (vertices) in the application model (directed graph) with edges
		 */
		application.addAppEdge("CAMERA", "motion_detector", 1000, 2000, "CAMERA", Tuple.UP, AppEdge.SENSOR); // adding edge from CAMERA (sensor) to Motion Detector module carrying tuples of type CAMERA
		application.addAppEdge("motion_detector", "masterController", 1000, 2000, "TaskToMaster", Tuple.UP, AppEdge.MODULE); // adding edge from Motion Detector to Object Detector module carrying tuples of type MOTION_VIDEO_STREAM
		application.addAppEdge("masterController", "worker_node", 1000, 2000, "TaskToNode", Tuple.UP, AppEdge.MODULE); // adding edge from Object Detector to User Interface module carrying tuples of type DETECTED_OBJECT
		application.addAppEdge("worker_node", "masterController", 100, 100, "ResponseToMaster", Tuple.DOWN, AppEdge.MODULE); // adding edge from Object Detector to Object Tracker module carrying tuples of type OBJECT_LOCATION
		application.addAppEdge("masterController", "user_interface", 100, 200, "DETECTED_OBJECT", Tuple.UP, AppEdge.MODULE); 
		application.addAppEdge("masterController", "PTZ_CONTROL", 100, 28, 100, "PTZ_PARAMS", Tuple.DOWN, AppEdge.ACTUATOR); // adding edge from Object Tracker to PTZ CONTROL (actuator) carrying tuples of type PTZ_PARAMS
		
		/*
		 * Defining the input-output relationships (represented by selectivity) of the application modules. 
		 */
		application.addTupleMapping("motion_detector", "CAMERA", "TaskToMaster", new FractionalSelectivity(1.0)); // 1.0 tuples of type MOTION_VIDEO_STREAM are emitted by Motion Detector module per incoming tuple of type CAMERA
		application.addTupleMapping("masterController", "TaskToMaster", "TaskToNode", new FractionalSelectivity(1.0));
		application.addTupleMapping("masterController", "ResponseToMaster", "PTZ_PARAMS", new FractionalSelectivity(1.0));
		application.addTupleMapping("worker_node", "TaskToNode", "ResponseToMaster", new FractionalSelectivity(1.0)); // 1.0 tuples of type OBJECT_LOCATION are emitted by Object Detector module per incoming tuple of type MOTION_VIDEO_STREAM

	
		/*
		 * Defining application loops (maybe incomplete loops) to monitor the latency of. 
		 * Here, we add two loops for monitoring : Motion Detector -> Object Detector -> Object Tracker and Object Tracker -> PTZ Control
		 */
		
		//final AppLoop loop1 = new AppLoop(new ArrayList<String>(){{add("motion_detector");add("object_detector");add("object_tracker");}});
		final AppLoop loop1 = new AppLoop(new ArrayList<String>(){{add("motion_detector");add("masterController");add("worker_node");}});
		final AppLoop loop2 = new AppLoop(new ArrayList<String>(){{add("worker_node");add("masterController");add("PTZ_CONTROL");}});
		final AppLoop loop3 = new AppLoop(new ArrayList<String>(){{add("worker_node");add("masterController");}});
		List<AppLoop> loops = new ArrayList<AppLoop>(){{add(loop1);add(loop2);add(loop3);}};
		//List<AppLoop> loops = new ArrayList<AppLoop>(){{add(loop1);}};
		
		application.setLoops(loops);
		
		
		return application;
	}

}
