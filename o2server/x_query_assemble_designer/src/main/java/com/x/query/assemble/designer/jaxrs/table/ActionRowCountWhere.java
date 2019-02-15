package com.x.query.assemble.designer.jaxrs.table;

import javax.persistence.EntityManager;

import org.apache.commons.lang3.StringUtils;

import com.x.base.core.container.EntityManagerContainer;
import com.x.base.core.container.factory.EntityManagerContainerFactory;
import com.x.base.core.entity.JpaObject;
import com.x.base.core.project.exception.ExceptionAccessDenied;
import com.x.base.core.project.exception.ExceptionEntityNotExist;
import com.x.base.core.project.http.ActionResult;
import com.x.base.core.project.http.EffectivePerson;
import com.x.base.core.project.jaxrs.WrapLong;
import com.x.query.assemble.designer.Business;
import com.x.query.assemble.designer.DynamicEntity;
import com.x.query.core.entity.schema.Table;

class ActionRowCountWhere extends BaseAction {

	ActionResult<Wo> execute(EffectivePerson effectivePerson, String tableFlag, String where)
			throws Exception {
		try (EntityManagerContainer emc = EntityManagerContainerFactory.instance().create()) {
			ActionResult<Wo> result = new ActionResult<>();
			Table table = emc.flag(tableFlag, Table.class);
			Business business = new Business(emc);
			if (null == table) {
				throw new ExceptionEntityNotExist(tableFlag, Table.class);
			}
			if (!business.editable(effectivePerson, table)) {
				throw new ExceptionAccessDenied(effectivePerson, table);
			}
			DynamicEntity dynamicEntity = new DynamicEntity(table.getName());
			Class<? extends JpaObject> clz = (Class<JpaObject>) Class.forName(dynamicEntity.className());
			EntityManager em = emc.get(clz);
			String sql = "SELECT count(o) FROM " + clz.getName() + " o";
			if (StringUtils.isNotBlank(where) && (!StringUtils.equals(where, EMPTY_SYMBOL))) {
				sql += " where (" + where + ")";
			}
			Long count = (Long) em.createQuery(sql).getSingleResult();
			Wo wo = new Wo();
			wo.setValue(count);
			result.setData(wo);
			return result;
		}
	}

	public static class Wo extends WrapLong {

	}

}