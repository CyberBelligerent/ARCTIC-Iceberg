package com.rahman.arctic.iceberg.services;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.rahman.arctic.iceberg.objects.computers.ArcticHost;
import com.rahman.arctic.iceberg.objects.computers.ArcticNetwork;
import com.rahman.arctic.iceberg.objects.computers.ArcticRouter;
import com.rahman.arctic.iceberg.objects.computers.ArcticSecurityGroup;
import com.rahman.arctic.iceberg.objects.computers.ArcticSecurityGroupRule;
import com.rahman.arctic.iceberg.objects.computers.ArcticVolume;
import com.rahman.arctic.shard.ShardManager;
import com.rahman.arctic.shard.configuration.persistence.ShardProfile;
import com.rahman.arctic.shard.objects.ArcticTask;
import com.rahman.arctic.shard.objects.abstraction.ArcticHostSO;
import com.rahman.arctic.shard.objects.abstraction.ArcticNetworkSO;
import com.rahman.arctic.shard.objects.abstraction.ArcticRouterSO;
import com.rahman.arctic.shard.objects.abstraction.ArcticSecurityGroupRuleSO;
import com.rahman.arctic.shard.objects.abstraction.ArcticSecurityGroupSO;
import com.rahman.arctic.shard.objects.abstraction.ArcticVolumeSO;

import lombok.Getter;
import lombok.Setter;

@Component
@Scope("prototype")
public class IcebergCreator extends Thread {
	
	private final ShardManager sm;
	private ExecutorService executorService = Executors.newFixedThreadPool(5);
	
	@Getter @Setter
	public ShardProfile profile;

	@Getter @Setter
	private boolean destroyMode = false;

	@Getter
	private List<ArcticTask<?, ?>> tasksToComplete = new ArrayList<>();
	
	public IcebergCreator(ShardManager shardManager) {
		sm = shardManager;
	}
	
	public void attemptCreation() {
		sm.createSession(profile);
	}
	
	public void createHost(ArcticHost ah) {
		ArcticHostSO ahso = new ArcticHostSO();
		ahso.setName(ah.getName());
		ahso.setIp(ah.getIp());
//		ahso.setImageId(ah.getImageId());
//		ahso.setFlavor(ah.getFlavorId());
		ahso.setRangeId(ah.getRangeId());
		ahso.setVolumes(ah.getVolumes());
		ahso.setNetworks(ah.getNetworks());
		ahso.setOsType(ah.getOsType());
//		ahso.setDefaultUser(ah.getDefaultUser());
//		ahso.setDefaultPassword(ah.getDefaultPassword());
//		ahso.setWantedIPs(ah.getWantedIPs());
		
		ahso.setExtraVariables(ah.getExtraVariables());
		
		sm.getSession(profile).createHost(ahso);
	}
	
	public void createNetwork(ArcticNetwork an) {
		ArcticNetworkSO anso = new ArcticNetworkSO();
		anso.setName(an.getNetName());
		anso.setIpCidr(an.getNetCidr());
		anso.setIpGateway(an.getGateway());
		anso.setIpRangeEnd(an.getNetEnd());
		anso.setIpRangeStart(an.getNetStart());
		anso.setRangeId(an.getRangeId());
		
		sm.getSession(profile).createNetwork(anso);
	}
	
	public void createSecurityGroup(ArcticSecurityGroup asg) {
		ArcticSecurityGroupSO asgso = new ArcticSecurityGroupSO();
		asgso.setDescription(asg.getDescription());
		asgso.setName(asg.getName());
		asgso.setRangeId(asg.getRangeId());
		
		sm.getSession(profile).createSecurityGroup(asgso);
	}
	
	public void createSecurityGroupRule(ArcticSecurityGroupRule asgr) {
		ArcticSecurityGroupRuleSO asgrso = new ArcticSecurityGroupRuleSO();
		asgrso.setDescription(asgr.getDescription());
		asgrso.setDirection(asgr.getDirection());
		asgrso.setEndPortRange(asgr.getEndPortRange());
		asgrso.setEth(asgr.getEth());
		asgrso.setName(asgr.getName());
		asgrso.setProtocol(asgr.getProtocol());
		asgrso.setRangeId(asgr.getRangeId());
		asgrso.setSecGroup(asgr.getSecGroup());
		asgrso.setStartPortRange(asgr.getStartPortRange());
		
		sm.getSession(profile).createSecurityGroupRule(asgrso);
	}
	
	public void createRouter(ArcticRouter ar) {
		ArcticRouterSO arso = new ArcticRouterSO();
		arso.setConnectedNetworkNames(ar.getNetworks());
		arso.setName(ar.getName());
		arso.setRangeId(ar.getRangeId());
		
		sm.getSession(profile).createRouter(arso);
	}
	
	public void createVolume(ArcticVolume av) {
		ArcticVolumeSO avso = new ArcticVolumeSO();
		avso.setBootable(av.isBootable());
		avso.setDescription(av.getDescription());
		avso.setImageId(av.getImageId());
		avso.setName(av.getName());
		avso.setRangeId(av.getRangeId());
		avso.setSize(av.getSize());

		sm.getSession(profile).createVolume(avso);
	}

	public void destroyHost(ArcticHost ah) {
		ArcticHostSO ahso = new ArcticHostSO();
		ahso.setName(ah.getName());
		ahso.setIp(ah.getIp());
		ahso.setRangeId(ah.getRangeId());
		ahso.setVolumes(ah.getVolumes());
		ahso.setNetworks(ah.getNetworks());
		ahso.setOsType(ah.getOsType());
		ahso.setExtraVariables(ah.getExtraVariables());
		ahso.setProviderId(ah.getProviderId());

		sm.getSession(profile).destroyHost(ahso);
	}

	public void destroyNetwork(ArcticNetwork an) {
		ArcticNetworkSO anso = new ArcticNetworkSO();
		anso.setName(an.getNetName());
		anso.setIpCidr(an.getNetCidr());
		anso.setIpGateway(an.getGateway());
		anso.setIpRangeEnd(an.getNetEnd());
		anso.setIpRangeStart(an.getNetStart());
		anso.setRangeId(an.getRangeId());
		anso.setProviderId(an.getProviderId());

		sm.getSession(profile).destroyNetwork(anso);
	}

	public void destroySecurityGroup(ArcticSecurityGroup asg) {
		ArcticSecurityGroupSO asgso = new ArcticSecurityGroupSO();
		asgso.setDescription(asg.getDescription());
		asgso.setName(asg.getName());
		asgso.setRangeId(asg.getRangeId());
		asgso.setProviderId(asg.getProviderId());

		sm.getSession(profile).destroySecurityGroup(asgso);
	}

	public void destroySecurityGroupRule(ArcticSecurityGroupRule asgr) {
		ArcticSecurityGroupRuleSO asgrso = new ArcticSecurityGroupRuleSO();
		asgrso.setDescription(asgr.getDescription());
		asgrso.setDirection(asgr.getDirection());
		asgrso.setEndPortRange(asgr.getEndPortRange());
		asgrso.setEth(asgr.getEth());
		asgrso.setName(asgr.getName());
		asgrso.setProtocol(asgr.getProtocol());
		asgrso.setRangeId(asgr.getRangeId());
		asgrso.setSecGroup(asgr.getSecGroup());
		asgrso.setStartPortRange(asgr.getStartPortRange());
		asgrso.setProviderId(asgr.getProviderId());

		sm.getSession(profile).destroySecurityGroupRule(asgrso);
	}

	public void destroyRouter(ArcticRouter ar) {
		ArcticRouterSO arso = new ArcticRouterSO();
		arso.setConnectedNetworkNames(ar.getNetworks());
		arso.setName(ar.getName());
		arso.setRangeId(ar.getRangeId());
		arso.setProviderId(ar.getProviderId());

		sm.getSession(profile).destroyRouter(arso);
	}

	public void destroyVolume(ArcticVolume av) {
		ArcticVolumeSO avso = new ArcticVolumeSO();
		avso.setBootable(av.isBootable());
		avso.setDescription(av.getDescription());
		avso.setImageId(av.getImageId());
		avso.setName(av.getName());
		avso.setRangeId(av.getRangeId());
		avso.setSize(av.getSize());
		avso.setProviderId(av.getProviderId());

		sm.getSession(profile).destroyVolume(avso);
	}

	// TODO: Needs to be fixed with user profiles and who is doing this
	public void run() {
		if (destroyMode) {
			tasksToComplete.addAll(sm.getSession(profile).getDestroyInstanceTasks().values());
			tasksToComplete.addAll(sm.getSession(profile).getDestroyNetworkTasks().values());
			tasksToComplete.addAll(sm.getSession(profile).getDestroyRouterTasks().values());
			tasksToComplete.addAll(sm.getSession(profile).getDestroySecurityGroupRuleTasks().values());
			tasksToComplete.addAll(sm.getSession(profile).getDestroySecurityGroupTasks().values());
			tasksToComplete.addAll(sm.getSession(profile).getDestroyVolumeTasks().values());
		} else {
			tasksToComplete.addAll(sm.getSession(profile).getInstanceTasks().values());
			tasksToComplete.addAll(sm.getSession(profile).getNetworkTasks().values());
			tasksToComplete.addAll(sm.getSession(profile).getRouterTasks().values());
			tasksToComplete.addAll(sm.getSession(profile).getSecurityGroupRuleTasks().values());
			tasksToComplete.addAll(sm.getSession(profile).getSecurityGroupTasks().values());
			tasksToComplete.addAll(sm.getSession(profile).getVolumeTasks().values());
		}

		PriorityQueue<ArcticTask<?, ?>> queue = new PriorityQueue<ArcticTask<?, ?>>(tasksToComplete.size(), new Comparator<ArcticTask<?, ?>>() {
			@Override
			public int compare(ArcticTask<?, ?> o1, ArcticTask<?, ?> o2) {
				return o1.getPriority() - o2.getPriority();
			}
		});
		queue.addAll(tasksToComplete);

		while(!queue.isEmpty()) {
			executorService.execute(queue.poll());
		}

		tasksToComplete.clear();
	}
	
}