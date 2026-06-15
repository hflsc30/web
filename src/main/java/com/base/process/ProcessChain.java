package com.base.process;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 处理器链，收集所有 RequestProcessor 并按 order 排序串联执行。
  * @author base
 * @since 2026-06-11
 */
@Slf4j
public record ProcessChain(List<RequestProcessor> processors) {

	public ProcessChain(List<RequestProcessor> processors) {
		this.processors = new ArrayList<>(processors);
		this.processors.sort(Comparator.comparingInt(RequestProcessor::order));
	}

	/**
	 * 执行前置处理链，返回 false 表示链路被中断。
	 *
	 * @param processorsToRun 要执行的处理器子集（null 表示全部执行）
	 */
	public boolean preProcess(RequestContext ctx, List<Class<? extends RequestProcessor>> processorsToRun) {
		for (RequestProcessor p : processors) {
			if (processorsToRun != null && !matches(p, processorsToRun)) {
				continue;
			}
			try {
				if (!p.preProcess(ctx)) {
					ctx.setChainBroken(true);
					return false;
				}
			} catch (Exception e) {
				log.error("Processor [{}] preProcess error", p.name(), e);
				ctx.setChainBroken(true);
				return false;
			}
		}
		return true;
	}

	/**
	 * 执行前置处理链（全部处理器）。
	 */
	public boolean preProcess(RequestContext ctx) {
		return preProcess(ctx, null);
	}

	/**
	 * 执行后置处理链（逆序，全部处理器）。
	 */
	public void postProcess(RequestContext ctx) {
		for (int i = processors.size() - 1; i >= 0; i--) {
			RequestProcessor p = processors.get(i);
			try {
				p.postProcess(ctx);
			} catch (Exception e) {
				log.error("Processor [{}] postProcess error", p.name(), e);
			}
		}
	}

	private boolean matches(RequestProcessor p, List<Class<? extends RequestProcessor>> filter) {
		for (Class<? extends RequestProcessor> clazz : filter) {
			if (clazz.isAssignableFrom(p.getClass())) {
				return true;
			}
		}
		return false;
	}
}
