package com.base.result;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.ArrayList;
import java.util.List;

/**
 * @author base
 * @since 2026-06-11
 */
public class PageRUtil {

	public static <T> PageR<List<T>> build(Page<T> page) {
		PageR<List<T>> result = new PageR<>();
		if (page != null) {
			Pagination pagination = new Pagination();
			pagination.setTotal(page.getTotal());
			pagination.setPages(page.getPages());
			pagination.setPageSize(page.getSize());
			pagination.setCurrentPage(page.getCurrent());

			result.setData(page.getRecords() == null ? new ArrayList<>() : page.getRecords());
			result.setPagination(pagination);
		}
		return result;
	}
}
