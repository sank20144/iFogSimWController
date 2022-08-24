package org.fog.utils;

import java.util.List;
import java.util.Random;

import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.entities.FogDevice;
import org.fog.entities.Tuple;

public class FController {
	
	//Used for placement strategy
	public static boolean CAPACITY;
	public static boolean CLOUD;
	public static boolean EDGEWARDS;
		
	
	// Used for Capacity Placement
	public static final int CPU_UTILIZATION = 1;
	public static final int BW_UTILIZATION = 2;
	public static final int POWER_UTILIZATION = 3;
	public static final int RAM_UTILIZATION = 4;
	// --
	public static List<FogDevice> fogDevices;
	
	public static int getRandomPlacement(List<Integer> parentIds) {
		Random r = new Random();
		int rando= r.nextInt(parentIds.size());
		return parentIds.get(rando);
	}
	public static double getCost(FogDevice device) {
		double util = device.getHost().getUtilizationOfCpu();
		double nextEnergy = device.getNextEnergyConsumption();
		double latency = device.getUplinkLatency();
		
		double cost = util + nextEnergy + latency;
		return cost;
	}
	
	public static int getCapacityPlacement(List<Integer> parentIds, int utilizationMode, Tuple tuple) {
		
		int id = parentIds.get(0);
		
		switch (utilizationMode) {	
		/** 
		 * Capacity placement based on lowest CPU utilization + energy score system
		 * */
		case CPU_UTILIZATION:
			
			//double clock = CloudSim.clock();
			//double luut = ((FogDevice)CloudSim.getEntity(parentIds.get(0))).getLuut();
			FogDevice firstDevice = (FogDevice)CloudSim.getEntity(parentIds.get(0));
			//double highestCpuUtilization = ((FogDevice)CloudSim.getEntity(parentIds.get(0))).getHost().getUtilizationOfCpu(); //Get Current CPU utilization of first Fog Device
			//double fe = ((FogDevice)CloudSim.getEntity(parentIds.get(0))).getNextEnergyConsumption(); //Get next energy consumption
			double highestCost = getCost(firstDevice);
			// Select the FogDevice with the lowest CPU utilization and return the ParentId
			for(Integer i: parentIds) { 
				FogDevice device = (FogDevice)CloudSim.getEntity(i);
				double cost = getCost(device);
				//System.out.print("ID:"+i+"|Energy:"+nextEnergy+"|Cost:"+cost + "| ");
				if (cost < highestCost) 
					id = i;
				if(cost > highestCost)
					highestCost = cost; 
			}
			
			break;
		case BW_UTILIZATION:
			double highestBwUtilization = ((FogDevice)CloudSim.getEntity(parentIds.get(0))).getHost().getUtilizationOfBw(); //Get Current BW utilization of first Fog Device
			
			for(Integer i: parentIds) {
				double bw = ((FogDevice)CloudSim.getEntity(i)).getHost().getUtilizationOfBw();
				if (bw < highestBwUtilization) 
					id = i;
				if(bw > highestBwUtilization)
					highestBwUtilization = bw;
			}
			break;
		case POWER_UTILIZATION:
			double highestPowerUtilization = ((FogDevice)CloudSim.getEntity(parentIds.get(0))).getHost().getPreviousPower(); //Get Power utilization of first Fog Device
			
			for(Integer i: parentIds) {
				double power = ((FogDevice)CloudSim.getEntity(i)).getHost().getPreviousPower();
				
				if (power < highestPowerUtilization) 
					id = i;
				if(power > highestPowerUtilization)
					highestPowerUtilization = power;
			}
			break;
		case RAM_UTILIZATION:
			double highestRamUtilization = ((FogDevice)CloudSim.getEntity(parentIds.get(0))).getHost().getUtilizationOfRam(); //Get Power utilization of first Fog Device
			
			for(Integer i: parentIds) {
				double ram = ((FogDevice)CloudSim.getEntity(i)).getHost().getUtilizationOfRam();
				if (ram < highestRamUtilization) 
					id = i;
				if(ram > highestRamUtilization)
					highestRamUtilization = ram;
			}
			break;
		default:
		}
		
		return id;
	}
	
	public static List<FogDevice> getFogDevices(){
		return fogDevices;
	}
	
}
