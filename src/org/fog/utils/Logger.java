package org.fog.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.application.AppModule;
import org.fog.entities.FogDevice;
import org.fog.entities.Tuple;
import org.fog.test.perfeval.Random_Placement;

public class Logger {
	
	public static final int ERROR = 1;
	public static final int DEBUG = 0;
	
	public static int LOG_LEVEL = Logger.DEBUG;
	private static DecimalFormat df = new DecimalFormat("#.00"); 

	public static boolean ENABLED = false;
	
	public static int iteration = 0; // Iteration counter for printTupleCsv()
	public static String fileName = null;
	
	
	
	public static FileWriter csvWriter = null;
	
	
	public static void setLogLevel(int level){
		Logger.LOG_LEVEL = level;
	}
	
	public static void debug(String name, String message){
		if(!ENABLED)
			return;
		if(Logger.LOG_LEVEL <= Logger.DEBUG)
			System.out.println(df.format(CloudSim.clock())+" : "+name+" : "+message);
	}
	public static void error(String name, String message){
		if(!ENABLED)
			return;
		if(Logger.LOG_LEVEL <= Logger.ERROR)
			System.out.println(df.format(CloudSim.clock())+" : "+name+" : "+message);
	}
	public static double getTotalEnergy() {
		double totalEnergy = 0;
		for (FogDevice fogDevice : FController.getFogDevices()) {
			totalEnergy += fogDevice.getEnergyConsumption();
		}
		return totalEnergy;
	}
	
	public static String getTupleDirection(Tuple tuple) {
		switch (tuple.getDirection()) {
		case Tuple.ACTUATOR:
			return "Actuator";
		case Tuple.DOWN:
			return "Down";
		case Tuple.UP:
			return "Up";
		default:
			return "N/A";
		}

	}
	////////////////// Special CSV Implementation ///////////////////
	
	// Prefix 'p' for FogDevice attributes / Prefix 't' for Tuple Attributes
	
	public static void printTupleToCsv(List<List<String>> rows) throws IOException {
		
		if(fileName == null) {
			String placementStrategy = FController.CLOUD ? "Cloud": FController.EDGEWARDS ? "Edgewards": FController.CAPACITY ? "Capacity" : "Random";
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");  
			LocalDateTime now = LocalDateTime.now();
			fileName = dtf.format(now) + "_" + placementStrategy;
			
		}
		
		String pwd = System.getProperty("user.dir") + "\\";
		csvWriter = new FileWriter(pwd + fileName + ".csv", true);
		
		if (iteration == 0) {
			csvWriter.append("Iteration #");
			csvWriter.append(",");
			csvWriter.append("Cloudlet_ID");
			csvWriter.append(",");
			csvWriter.append("Cloudlet_length");
			csvWriter.append(",");
			csvWriter.append("Cloudlet_filesize");
			csvWriter.append(",");
			csvWriter.append("Cloudlet_outputsize");
			csvWriter.append(",");
			csvWriter.append("CostPerBw");
			csvWriter.append(",");
			csvWriter.append("SourceDeviceId");
			csvWriter.append(",");
			csvWriter.append("DestinationDeviceId");
			csvWriter.append(",");
			csvWriter.append("AccumulatedBwCost");
			csvWriter.append(",");
			csvWriter.append("DestModelName");
			csvWriter.append(",");
			csvWriter.append("Direction");
			csvWriter.append(",");
			csvWriter.append("Tuple Type");
			csvWriter.append(",");
			csvWriter.append("ExecStartTime");
			csvWriter.append(",");
			csvWriter.append("FinishTime");
			csvWriter.append(",");
			csvWriter.append("UtilizationModelBw");
			csvWriter.append(",");
			csvWriter.append("UtilizationModelCpu");
			csvWriter.append(",");
			csvWriter.append("UtilizationModelRam");
			csvWriter.append(",");
			
			csvWriter.append("NodeName");  
			csvWriter.append(",");
			csvWriter.append("Mips");
			csvWriter.append(",");
			csvWriter.append("Ram");
			csvWriter.append(",");
			csvWriter.append("UpBw");
			csvWriter.append(",");
			csvWriter.append("DownBw");
			csvWriter.append(",");
			csvWriter.append("Level");
			csvWriter.append(",");
			csvWriter.append("RatePerMips");
			csvWriter.append(",");
			csvWriter.append("BusyPower");
			csvWriter.append(",");
			csvWriter.append("IdlePower");
			csvWriter.append(",");
			csvWriter.append("Node Current Power");
			csvWriter.append(",");
			csvWriter.append("Cpu utilization");
			csvWriter.append(",");
			csvWriter.append("Bw utilization");
			csvWriter.append(",");
			csvWriter.append("CpuMips utilization");
			csvWriter.append(",");
			csvWriter.append("Ram utilization");
			csvWriter.append(",");
			csvWriter.append("Mips utilization");
			csvWriter.append(",");
			csvWriter.append("Node Action");
			csvWriter.append(",");
			csvWriter.append("o	Node Energy consumed ");
			csvWriter.append(",");
			csvWriter.append("o	Total Energy consumed ");
			csvWriter.append(",");
			csvWriter.append("o	Total network usage ");
			csvWriter.append("\n");
		}
		csvWriter.append(iteration + ",");
		
		for (List<String> rowData : rows) {
			csvWriter.append(String.join(",", rowData));
			csvWriter.append("\n");
		}

		csvWriter.flush();
		csvWriter.close();
		
		iteration++;
	}
	
	public static String getNodeAction(Tuple tuple, FogDevice device) {
		Map<String, List<String>> appToModulesMap = device.getAppToModulesMap();
		if (appToModulesMap.containsKey(tuple.getAppId())) {
			if (appToModulesMap.get(tuple.getAppId()).contains(tuple.getDestModuleName())) {
				int vmId = -1;
				for (Vm vm : device.getHost().getVmList()) {
					if (((AppModule) vm).getName().equals(tuple.getDestModuleName()))
						vmId = vm.getId();
				}
				if (vmId < 0 || (tuple.getModuleCopyMap().containsKey(tuple.getDestModuleName())
						&& tuple.getModuleCopyMap().get(tuple.getDestModuleName()) != vmId)) {
					return "No VM in App";
				}
				
				return "Process";
				
			} else if (tuple.getDestModuleName() != null) {
				if (tuple.getDirection() == Tuple.UP)
				{
					//System.err.println(getName() +  " - Tuple will be sent up to search for " +  tuple.getDestModuleName());
					return "Send Up 1";
				}
				else if (tuple.getDirection() == Tuple.DOWN) {
					
						return "Send Down";
					
				}
			} else {
				//System.err.println(getName() +  " - Tuple will be sent up as there is no destination specified or the module is not available on existing node" + tuple.getDestModuleName());
				return "Send up";
			}
		} else {
			//System.err.println(getName() +  " - Tuple will be sent up/down as there is the module is not available on existing node" + tuple.getDestModuleName());
			if (tuple.getDirection() == Tuple.UP)
				return "Send Up 2";
			else if (tuple.getDirection() == Tuple.DOWN) {
				
					return "Send Down";
			}
		}
		return "Actuator params";
	}
	
}
