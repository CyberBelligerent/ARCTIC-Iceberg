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
import com.rahman.arctic.shard.objects.ArcticHostSO;
import com.rahman.arctic.shard.objects.ArcticNetworkSO;
import com.rahman.arctic.shard.objects.ArcticRouterSO;
import com.rahman.arctic.shard.objects.ArcticSecurityGroupRuleSO;
import com.rahman.arctic.shard.objects.ArcticSecurityGroupSO;
import com.rahman.arctic.shard.objects.ArcticTask;
import com.rahman.arctic.shard.objects.ArcticVolumeSO;
import com.rahman.arctic.shard.shards.ShardProviderTmpl;

import lombok.Getter;
import lombok.Setter;

@Component
@Scope("prototype")
public class IcebergCreator extends Thread {

	@Autowired
	private ShardManager shardManager;
	
	private ExecutorService executorService = Executors.newFixedThreadPool(5);
	
	@Getter @Setter
	@Getter
	private List<ArcticTask<?, ?>> tasksToComplete = new ArrayList<>();
	
	private final ShardProviderTmpl<?> provider;
	
	public IcebergCreator() {
		provider = shardManager.getPrimaryShard();
	}
	
	public void createHost(ArcticHost ah) {
		ArcticHostSO ahso = new ArcticHostSO();
		ahso.setName(ah.getName());
		ahso.setIp(ah.getIp());
		ahso.setFlavor(ah.getFlavorId());
		ahso.setRangeId(ah.getRangeId());
		ahso.setVolumes(ah.getVolumes());
		ahso.setNetworks(ah.getNetworks());
		
		provider.createHost(ahso);
	}
	
	public void createNetwork(ArcticNetwork an) {
		ArcticNetworkSO anso = new ArcticNetworkSO();
		anso.setName(an.getNetName());
		anso.setIpCidr(an.getNetCidr());
		anso.setIpGateway(an.getGateway());
		anso.setIpRangeEnd(an.getNetEnd());
		anso.setIpRangeStart(an.getNetStart());
		anso.setRangeId(an.getRangeId());
		
		provider.createNetwork(anso);
	}
	
	public void createSecurityGroup(ArcticSecurityGroup asg) {
		ArcticSecurityGroupSO asgso = new ArcticSecurityGroupSO();
		asgso.setDescription(asg.getDescription());
		asgso.setName(asg.getName());
		asgso.setRangeId(asg.getRangeId());
		
		provider.createSecurityGroup(asgso);
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
		
		provider.createSecurityGroupRule(asgrso);
	}
	
	public void createRouter(ArcticRouter ar) {
		ArcticRouterSO arso = new ArcticRouterSO();
		arso.setConnectedNetworkNames(ar.getNetworks());
		arso.setName(ar.getName());
		arso.setRangeId(ar.getRangeId());
		
		provider.createRouter(arso);
	}
	
	public void createVolume(ArcticVolume av) {
		ArcticVolumeSO avso = new ArcticVolumeSO();
		avso.setBootable(av.isBootable());
		avso.setDescription(av.getDescription());
		avso.setImageId(av.getImageId());
		avso.setName(av.getName());
		avso.setRangeId(av.getRangeId());
		avso.setSize(av.getSize());
		
		provider.createVolume(avso);
	}
	
	public void run() {
		tasksToComplete.addAll(provider.getInstanceTasks().values());
		tasksToComplete.addAll(provider.getNetworkTasks().values());
		tasksToComplete.addAll(provider.getRouterTasks().values());
		tasksToComplete.addAll(provider.getSecurityGroupRuleTasks().values());
		tasksToComplete.addAll(provider.getSecurityGroupTasks().values());
		tasksToComplete.addAll(provider.getVolumeTasks().values());
		
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