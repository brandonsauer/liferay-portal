/**
 * Copyright (c) 2000-2008 Liferay, Inc. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.liferay.portlet.wiki.service.persistence;

import com.liferay.portal.SystemException;
import com.liferay.portal.kernel.bean.InitializingBean;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.FinderCacheUtil;
import com.liferay.portal.kernel.dao.orm.Query;
import com.liferay.portal.kernel.dao.orm.QueryPos;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.dao.orm.Session;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.OrderByComparator;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.uuid.PortalUUIDUtil;
import com.liferay.portal.model.ModelListener;
import com.liferay.portal.service.persistence.impl.BasePersistenceImpl;

import com.liferay.portlet.wiki.NoSuchPageException;
import com.liferay.portlet.wiki.model.WikiPage;
import com.liferay.portlet.wiki.model.impl.WikiPageImpl;
import com.liferay.portlet.wiki.model.impl.WikiPageModelImpl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * <a href="WikiPagePersistenceImpl.java.html"><b><i>View Source</i></b></a>
 *
 * @author Brian Wing Shun Chan
 *
 */
public class WikiPagePersistenceImpl extends BasePersistenceImpl
	implements WikiPagePersistence, InitializingBean {
	public WikiPage create(long pageId) {
		WikiPage wikiPage = new WikiPageImpl();

		wikiPage.setNew(true);
		wikiPage.setPrimaryKey(pageId);

		String uuid = PortalUUIDUtil.generate();

		wikiPage.setUuid(uuid);

		return wikiPage;
	}

	public WikiPage remove(long pageId)
		throws NoSuchPageException, SystemException {
		Session session = null;

		try {
			session = openSession();

			WikiPage wikiPage = (WikiPage)session.get(WikiPageImpl.class,
					new Long(pageId));

			if (wikiPage == null) {
				if (_log.isWarnEnabled()) {
					_log.warn("No WikiPage exists with the primary key " +
						pageId);
				}

				throw new NoSuchPageException(
					"No WikiPage exists with the primary key " + pageId);
			}

			return remove(wikiPage);
		}
		catch (NoSuchPageException nsee) {
			throw nsee;
		}
		catch (Exception e) {
			throw processException(e);
		}
		finally {
			closeSession(session);
		}
	}

	public WikiPage remove(WikiPage wikiPage) throws SystemException {
		if (_listeners.length > 0) {
			for (ModelListener listener : _listeners) {
				listener.onBeforeRemove(wikiPage);
			}
		}

		wikiPage = removeImpl(wikiPage);

		if (_listeners.length > 0) {
			for (ModelListener listener : _listeners) {
				listener.onAfterRemove(wikiPage);
			}
		}

		return wikiPage;
	}

	protected WikiPage removeImpl(WikiPage wikiPage) throws SystemException {
		Session session = null;

		try {
			session = openSession();

			session.delete(wikiPage);

			session.flush();

			return wikiPage;
		}
		catch (Exception e) {
			throw processException(e);
		}
		finally {
			closeSession(session);

			FinderCacheUtil.clearCache(WikiPage.class.getName());
		}
	}

	/**
	 * @deprecated Use <code>update(WikiPage wikiPage, boolean merge)</code>.
	 */
	public WikiPage update(WikiPage wikiPage) throws SystemException {
		if (_log.isWarnEnabled()) {
			_log.warn(
				"Using the deprecated update(WikiPage wikiPage) method. Use update(WikiPage wikiPage, boolean merge) instead.");
		}

		return update(wikiPage, false);
	}

	/**
	 * Add, update, or merge, the entity. This method also calls the model
	 * listeners to trigger the proper events associated with adding, deleting,
	 * or updating an entity.
	 *
	 * @param        wikiPage the entity to add, update, or merge
	 * @param        merge boolean value for whether to merge the entity. The
	 *                default value is false. Setting merge to true is more
	 *                expensive and should only be true when wikiPage is
	 *                transient. See LEP-5473 for a detailed discussion of this
	 *                method.
	 * @return        true if the portlet can be displayed via Ajax
	 */
	public WikiPage update(WikiPage wikiPage, boolean merge)
		throws SystemException {
		boolean isNew = wikiPage.isNew();

		if (_listeners.length > 0) {
			for (ModelListener listener : _listeners) {
				if (isNew) {
					listener.onBeforeCreate(wikiPage);
				}
				else {
					listener.onBeforeUpdate(wikiPage);
				}
			}
		}

		wikiPage = updateImpl(wikiPage, merge);

		if (_listeners.length > 0) {
			for (ModelListener listener : _listeners) {
				if (isNew) {
					listener.onAfterCreate(wikiPage);
				}
				else {
					listener.onAfterUpdate(wikiPage);
				}
			}
		}

		return wikiPage;
	}

	public WikiPage updateImpl(
		com.liferay.portlet.wiki.model.WikiPage wikiPage, boolean merge)
		throws SystemException {
		if (Validator.isNull(wikiPage.getUuid())) {
			String uuid = PortalUUIDUtil.generate();

			wikiPage.setUuid(uuid);
		}

		Session session = null;

		try {
			session = openSession();

			if (merge) {
				session.merge(wikiPage);
			}
			else {
				if (wikiPage.isNew()) {
					session.save(wikiPage);
				}
			}

			session.flush();

			wikiPage.setNew(false);

			return wikiPage;
		}
		catch (Exception e) {
			throw processException(e);
		}
		finally {
			closeSession(session);

			FinderCacheUtil.clearCache(WikiPage.class.getName());
		}
	}

	public WikiPage findByPrimaryKey(long pageId)
		throws NoSuchPageException, SystemException {
		WikiPage wikiPage = fetchByPrimaryKey(pageId);

		if (wikiPage == null) {
			if (_log.isWarnEnabled()) {
				_log.warn("No WikiPage exists with the primary key " + pageId);
			}

			throw new NoSuchPageException(
				"No WikiPage exists with the primary key " + pageId);
		}

		return wikiPage;
	}

	public WikiPage fetchByPrimaryKey(long pageId) throws SystemException {
		Session session = null;

		try {
			session = openSession();

			return (WikiPage)session.get(WikiPageImpl.class, new Long(pageId));
		}
		catch (Exception e) {
			throw processException(e);
		}
		finally {
			closeSession(session);
		}
	}

	public List<WikiPage> findByUuid(String uuid) throws SystemException {
		boolean finderClassNameCacheEnabled = WikiPageModelImpl.CACHE_ENABLED;
		String finderClassName = WikiPage.class.getName();
		String finderMethodName = "findByUuid";
		String[] finderParams = new String[] { String.class.getName() };
		Object[] finderArgs = new Object[] { uuid };

		Object result = null;

		if (finderClassNameCacheEnabled) {
			result = FinderCacheUtil.getResult(finderClassName,
					finderMethodName, finderParams, finderArgs, this);
		}

		if (result == null) {
			Session session = null;

			try {
				session = openSession();

				StringBuilder query = new StringBuilder();

				query.append(
					"FROM com.liferay.portlet.wiki.model.WikiPage WHERE ");

				if (uuid == null) {
					query.append("uuid_ IS NULL");
				}
				else {
					query.append("uuid_ = ?");
				}

				query.append(" ");

				query.append("ORDER BY ");

				query.append("nodeId ASC, ");
				query.append("title ASC, ");
				query.append("version ASC");

				Query q = session.createQuery(query.toString());

				QueryPos qPos = QueryPos.getInstance(q);

				if (uuid != null) {
					qPos.add(uuid);
				}

				List<WikiPage> list = q.list();

				FinderCacheUtil.putResult(finderClassNameCacheEnabled,
					finderClassName, finderMethodName, finderParams,
					finderArgs, list);

				return list;
			}
			catch (Exception e) {
				throw processException(e);
			}
			finally {
				closeSession(session);
			}
		}
		else {
			return (List<WikiPage>)result;
		}
	}

	public List<WikiPage> findByUuid(String uuid, int start, int end)
		throws SystemException {
		return findByUuid(uuid, start, end, null);
	}

	public List<WikiPage> findByUuid(String uuid, int start, int end,
		OrderByComparator obc) throws SystemException {
		boolean finderClassNameCacheEnabled = WikiPageModelImpl.CACHE_ENABLED;
		String finderClassName = WikiPage.class.getName();
		String finderMethodName = "findByUuid";
		String[] finderParams = new String[] {
				String.class.getName(),
				
				"java.lang.Integer", "java.lang.Integer",
				"com.liferay.portal.kernel.util.OrderByComparator"
			};
		Object[] finderArgs = new Object[] {
				uuid,
				
				String.valueOf(start), String.valueOf(end), String.valueOf(obc)
			};

		Object result = null;

		if (finderClassNameCacheEnabled) {
			result = FinderCacheUtil.getResult(finderClassName,
					finderMethodName, finderParams, finderArgs, this);
		}

		if (result == null) {
			Session session = null;

			try {
				session = openSession();

				StringBuilder query = new StringBuilder();

				query.append(
					"FROM com.liferay.portlet.wiki.model.WikiPage WHERE ");

				if (uuid == null) {
					query.append("uuid_ IS NULL");
				}
				else {
					query.append("uuid_ = ?");
				}

				query.append(" ");

				if (obc != null) {
					query.append("ORDER BY ");
					query.append(obc.getOrderBy());
				}

				else {
					query.append("ORDER BY ");

					query.append("nodeId ASC, ");
					query.append("title ASC, ");
					query.append("version ASC");
				}

				Query q = session.createQuery(query.toString());

				QueryPos qPos = QueryPos.getInstance(q);

				if (uuid != null) {
					qPos.add(uuid);
				}

				List<WikiPage> list = (List<WikiPage>)QueryUtil.list(q,
						getDialect(), start, end);

				FinderCacheUtil.putResult(finderClassNameCacheEnabled,
					finderClassName, finderMethodName, finderParams,
					finderArgs, list);

				return list;
			}
			catch (Exception e) {
				throw processException(e);
			}
			finally {
				closeSession(session);
			}
		}
		else {
			return (List<WikiPage>)result;
		}
	}

	public WikiPage findByUuid_First(String uuid, OrderByComparator obc)
		throws NoSuchPageException, SystemException {
		List<WikiPage> list = findByUuid(uuid, 0, 1, obc);

		if (list.size() == 0) {
			StringBuilder msg = new StringBuilder();

			msg.append("No WikiPage exists with the key {");

			msg.append("uuid=" + uuid);

			msg.append(StringPool.CLOSE_CURLY_BRACE);

			throw new NoSuchPageException(msg.toString());
		}
		else {
			return list.get(0);
		}
	}

	public WikiPage findByUuid_Last(String uuid, OrderByComparator obc)
		throws NoSuchPageException, SystemException {
		int count = countByUuid(uuid);

		List<WikiPage> list = findByUuid(uuid, count - 1, count, obc);

		if (list.size() == 0) {
			StringBuilder msg = new StringBuilder();

			msg.append("No WikiPage exists with the key {");

			msg.append("uuid=" + uuid);

			msg.append(StringPool.CLOSE_CURLY_BRACE);

			throw new NoSuchPageException(msg.toString());
		}
		else {
			return list.get(0);
		}
	}

	public WikiPage[] findByUuid_PrevAndNext(long pageId, String uuid,
		OrderByComparator obc) throws NoSuchPageException, SystemException {
		WikiPage wikiPage = findByPrimaryKey(pageId);

		int count = countByUuid(uuid);

		Session session = null;

		try {
			session = openSession();

			StringBuilder query = new StringBuilder();

			query.append("FROM com.liferay.portlet.wiki.model.WikiPage WHERE ");

			if (uuid == null) {
				query.append("uuid_ IS NULL");
			}
			else {
				query.append("uuid_ = ?");
			}

			query.append(" ");

			if (obc != null) {
				query.append("ORDER BY ");
				query.append(obc.getOrderBy());
			}

			else {
				query.append("ORDER BY ");

				query.append("nodeId ASC, ");
				query.append("title ASC, ");
				query.append("version ASC");
			}

			Query q = session.createQuery(query.toString());

			QueryPos qPos = QueryPos.getInstance(q);

			if (uuid != null) {
				qPos.add(uuid);
			}

			Object[] objArray = QueryUtil.getPrevAndNext(q, count, obc, wikiPage);

			WikiPage[] array = new WikiPageImpl[3];

			array[0] = (WikiPage)objArray[0];
			array[1] = (WikiPage)objArray[1];
			array[2] = (WikiPage)objArray[2];

			return array;
		}
		catch (Exception e) {
			throw processException(e);
		}
		finally {
			closeSession(session);
		}
	}

	public List<WikiPage> findByNodeId(long nodeId) throws SystemException {
		boolean finderClassNameCacheEnabled = WikiPageModelImpl.CACHE_ENABLED;
		String finderClassName = WikiPage.class.getName();
		String finderMethodName = "findByNodeId";
		String[] finderParams = new String[] { Long.class.getName() };
		Object[] finderArgs = new Object[] { new Long(nodeId) };

		Object result = null;

		if (finderClassNameCacheEnabled) {
			result = FinderCacheUtil.getResult(finderClassName,
					finderMethodName, finderParams, finderArgs, this);
		}

		if (result == null) {
			Session session = null;

			try {
				session = openSession();

				StringBuilder query = new StringBuilder();

				query.append(
					"FROM com.liferay.portlet.wiki.model.WikiPage WHERE ");

				query.append("nodeId = ?");

				query.append(" ");

				query.append("ORDER BY ");

				query.append("nodeId ASC, ");
				query.append("title ASC, ");
				query.append("version ASC");

				Query q = session.createQuery(query.toString());

				QueryPos qPos = QueryPos.getInstance(q);

				qPos.add(nodeId);

				List<WikiPage> list = q.list();

				FinderCacheUtil.putResult(finderClassNameCacheEnabled,
					finderClassName, finderMethodName, finderParams,
					finderArgs, list);

				return list;
			}
			catch (Exception e) {
				throw processException(e);
			}
			finally {
				closeSession(session);
			}
		}
		else {
			return (List<WikiPage>)result;
		}
	}

	public List<WikiPage> findByNodeId(long nodeId, int start, int end)
		throws SystemException {
		return findByNodeId(nodeId, start, end, null);
	}

	public List<WikiPage> findByNodeId(long nodeId, int start, int end,
		OrderByComparator obc) throws SystemException {
		boolean finderClassNameCacheEnabled = WikiPageModelImpl.CACHE_ENABLED;
		String finderClassName = WikiPage.class.getName();
		String finderMethodName = "findByNodeId";
		String[] finderParams = new String[] {
				Long.class.getName(),
				
				"java.lang.Integer", "java.lang.Integer",
				"com.liferay.portal.kernel.util.OrderByComparator"
			};
		Object[] finderArgs = new Object[] {
				new Long(nodeId),
				
				String.valueOf(start), String.valueOf(end), String.valueOf(obc)
			};

		Object result = null;

		if (finderClassNameCacheEnabled) {
			result = FinderCacheUtil.getResult(finderClassName,
					finderMethodName, finderParams, finderArgs, this);
		}

		if (result == null) {
			Session session = null;

			try {
				session = openSession();

				StringBuilder query = new StringBuilder();

				query.append(
					"FROM com.liferay.portlet.wiki.model.WikiPage WHERE ");

				query.append("nodeId = ?");

				query.append(" ");

				if (obc != null) {
					query.append("ORDER BY ");
					query.append(obc.getOrderBy());
				}

				else {
					query.append("ORDER BY ");

					query.append("nodeId ASC, ");
					query.append("title ASC, ");
					query.append("version ASC");
				}

				Query q = session.createQuery(query.toString());

				QueryPos qPos = QueryPos.getInstance(q);

				qPos.add(nodeId);

				List<WikiPage> list = (List<WikiPage>)QueryUtil.list(q,
						getDialect(), start, end);

				FinderCacheUtil.putResult(finderClassNameCacheEnabled,
					finderClassName, finderMethodName, finderParams,
					finderArgs, list);

				return list;
			}
			catch (Exception e) {
				throw processException(e);
			}
			finally {
				closeSession(session);
			}
		}
		else {
			return (List<WikiPage>)result;
		}
	}

	public WikiPage findByNodeId_First(long nodeId, OrderByComparator obc)
		throws NoSuchPageException, SystemException {
		List<WikiPage> list = findByNodeId(nodeId, 0, 1, obc);

		if (list.size() == 0) {
			StringBuilder msg = new StringBuilder();

			msg.append("No WikiPage exists with the key {");

			msg.append("nodeId=" + nodeId);

			msg.append(StringPool.CLOSE_CURLY_BRACE);

			throw new NoSuchPageException(msg.toString());
		}
		else {
			return list.get(0);
		}
	}

	public WikiPage findByNodeId_Last(long nodeId, OrderByComparator obc)
		throws NoSuchPageException, SystemException {
		int count = countByNodeId(nodeId);

		List<WikiPage> list = findByNodeId(nodeId, count - 1, count, obc);

		if (list.size() == 0) {
			StringBuilder msg = new StringBuilder();

			msg.append("No WikiPage exists with the key {");

			msg.append("nodeId=" + nodeId);

			msg.append(StringPool.CLOSE_CURLY_BRACE);

			throw new NoSuchPageException(msg.toString());
		}
		else {
			return list.get(0);
		}
	}

	public WikiPage[] findByNodeId_PrevAndNext(long pageId, long nodeId,
		OrderByComparator obc) throws NoSuchPageException, SystemException {
		WikiPage wikiPage = findByPrimaryKey(pageId);

		int count = countByNodeId(nodeId);

		Session session = null;

		try {
			session = openSession();

			StringBuilder query = new StringBuilder();

			query.append("FROM com.liferay.portlet.wiki.model.WikiPage WHERE ");

			query.append("nodeId = ?");

			query.append(" ");

			if (obc != null) {
				query.append("ORDER BY ");
				query.append(obc.getOrderBy());
			}

			else {
				query.append("ORDER BY ");

				query.append("nodeId ASC, ");
				query.append("title ASC, ");
				query.append("version ASC");
			}

			Query q = session.createQuery(query.toString());

			QueryPos qPos = QueryPos.getInstance(q);

			qPos.add(nodeId);

			Object[] objArray = QueryUtil.getPrevAndNext(q, count, obc, wikiPage);

			WikiPage[] array = new WikiPageImpl[3];

			array[0] = (WikiPage)objArray[0];
			array[1] = (WikiPage)objArray[1];
			array[2] = (WikiPage)objArray[2];

			return array;
		}
		catch (Exception e) {
			throw processException(e);
		}
		finally {
			closeSession(session);
		}
	}

	public List<WikiPage> findByFormat(String format) throws SystemException {
		boolean finderClassNameCacheEnabled = WikiPageModelImpl.CACHE_ENABLED;
		String finderClassName = WikiPage.class.getName();
		String finderMethodName = "findByFormat";
		String[] finderParams = new String[] { String.class.getName() };
		Object[] finderArgs = new Object[] { format };

		Object result = null;

		if (finderClassNameCacheEnabled) {
			result = FinderCacheUtil.getResult(finderClassName,
					finderMethodName, finderParams, finderArgs, this);
		}

		if (result == null) {
			Session session = null;

			try {
				session = openSession();

				StringBuilder query = new StringBuilder();

				query.append(
					"FROM com.liferay.portlet.wiki.model.WikiPage WHERE ");

				if (format == null) {
					query.append("format IS NULL");
				}
				else {
					query.append("format = ?");
				}

				query.append(" ");

				query.append("ORDER BY ");

				query.append("nodeId ASC, ");
				query.append("title ASC, ");
				query.append("version ASC");

				Query q = session.createQuery(query.toString());

				QueryPos qPos = QueryPos.getInstance(q);

				if (format != null) {
					qPos.add(format);
				}

				List<WikiPage> list = q.list();

				FinderCacheUtil.putResult(finderClassNameCacheEnabled,
					finderClassName, finderMethodName, finderParams,
					finderArgs, list);

				return list;
			}
			catch (Exception e) {
				throw processException(e);
			}
			finally {
				closeSession(session);
			}
		}
		else {
			return (List<WikiPage>)result;
		}
	}

	public List<WikiPage> findByFormat(String format, int start, int end)
		throws SystemException {
		return findByFormat(format, start, end, null);
	}

	public List<WikiPage> findByFormat(String format, int start, int end,
		OrderByComparator obc) throws SystemException {
		boolean finderClassNameCacheEnabled = WikiPageModelImpl.CACHE_ENABLED;
		String finderClassName = WikiPage.class.getName();
		String finderMethodName = "findByFormat";
		String[] finderParams = new String[] {
				String.class.getName(),
				
				"java.lang.Integer", "java.lang.Integer",
				"com.liferay.portal.kernel.util.OrderByComparator"
			};
		Object[] finderArgs = new Object[] {
				format,
				
				String.valueOf(start), String.valueOf(end), String.valueOf(obc)
			};

		Object result = null;

		if (finderClassNameCacheEnabled) {
			result = FinderCacheUtil.getResult(finderClassName,
					finderMethodName, finderParams, finderArgs, this);
		}

		if (result == null) {
			Session session = null;

			try {
				session = openSession();

				StringBuilder query = new StringBuilder();

				query.append(
					"FROM com.liferay.portlet.wiki.model.WikiPage WHERE ");

				if (format == null) {
					query.append("format IS NULL");
				}
				else {
					query.append("format = ?");
				}

				query.append(" ");

				if (obc != null) {
					query.append("ORDER BY ");
					query.append(obc.getOrderBy());
				}

				else {
					query.append("ORDER BY ");

					query.append("nodeId ASC, ");
					query.append("title ASC, ");
					query.append("version ASC");
				}

				Query q = session.createQuery(query.toString());

				QueryPos qPos = QueryPos.getInstance(q);

				if (format != null) {
					qPos.add(format);
				}

				List<WikiPage> list = (List<WikiPage>)QueryUtil.list(q,
						getDialect(), start, end);

				FinderCacheUtil.putResult(finderClassNameCacheEnabled,
					finderClassName, finderMethodName, finderParams,
					finderArgs, list);

				return list;
			}
			catch (Exception e) {
				throw processException(e);
			}
			finally {
				closeSession(session);
			}
		}
		else {
			return (List<WikiPage>)result;
		}
	}

	public WikiPage findByFormat_First(String format, OrderByComparator obc)
		throws NoSuchPageException, SystemException {
		List<WikiPage> list = findByFormat(format, 0, 1, obc);

		if (list.size() == 0) {
			StringBuilder msg = new StringBuilder();

			msg.append("No WikiPage exists with the key {");

			msg.append("format=" + format);

			msg.append(StringPool.CLOSE_CURLY_BRACE);

			throw new NoSuchPageException(msg.toString());
		}
		else {
			return list.get(0);
		}
	}

	public WikiPage findByFormat_Last(String format, OrderByComparator obc)
		throws NoSuchPageException, SystemException {
		int count = countByFormat(format);

		List<WikiPage> list = findByFormat(format, count - 1, count, obc);

		if (list.size() == 0) {
			StringBuilder msg = new StringBuilder();

			msg.append("No WikiPage exists with the key {");

			msg.append("format=" + format);

			msg.append(StringPool.CLOSE_CURLY_BRACE);

			throw new NoSuchPageException(msg.toString());
		}
		else {
			return list.get(0);
		}
	}

	public WikiPage[] findByFormat_PrevAndNext(long pageId, String format,
		OrderByComparator obc) throws NoSuchPageException, SystemException {
		WikiPage wikiPage = findByPrimaryKey(pageId);

		int count = countByFormat(format);

		Session session = null;

		try {
			session = openSession();

			StringBuilder query = new StringBuilder();

			query.append("FROM com.liferay.portlet.wiki.model.WikiPage WHERE ");

			if (format == null) {
				query.append("format IS NULL");
			}
			else {
				query.append("format = ?");
			}

			query.append(" ");

			if (obc != null) {
				query.append("ORDER BY ");
				query.append(obc.getOrderBy());
			}

			else {
				query.append("ORDER BY ");

				query.append("nodeId ASC, ");
				query.append("title ASC, ");
				query.append("version ASC");
			}

			Query q = session.createQuery(query.toString());

			QueryPos qPos = QueryPos.getInstance(q);

			if (format != null) {
				qPos.add(format);
			}

			Object[] objArray = QueryUtil.getPrevAndNext(q, count, obc, wikiPage);

			WikiPage[] array = new WikiPageImpl[3];

			array[0] = (WikiPage)objArray[0];
			array[1] = (WikiPage)objArray[1];
			array[2] = (WikiPage)objArray[2];

			return array;
		}
		catch (Exception e) {
			throw processException(e);
		}
		finally {
			closeSession(session);
		}
	}

	public List<WikiPage> findByN_T(long nodeId, String title)
		throws SystemException {
		boolean finderClassNameCacheEnabled = WikiPageModelImpl.CACHE_ENABLED;
		String finderClassName = WikiPage.class.getName();
		String finderMethodName = "findByN_T";
		String[] finderParams = new String[] {
				Long.class.getName(), String.class.getName()
			};
		Object[] finderArgs = new Object[] { new Long(nodeId), title };

		Object result = null;

		if (finderClassNameCacheEnabled) {
			result = FinderCacheUtil.getResult(finderClassName,
					finderMethodName, finderParams, finderArgs, this);
		}

		if (result == null) {
			Session session = null;

			try {
				session = openSession();

				StringBuilder query = new StringBuilder();

				query.append(
					"FROM com.liferay.portlet.wiki.model.WikiPage WHERE ");

				query.append("nodeId = ?");

				query.append(" AND ");

				if (title == null) {
					query.append("title IS NULL");
				}
				else {
					query.append("title = ?");
				}

				query.append(" ");

				query.append("ORDER BY ");

				query.append("nodeId ASC, ");
				query.append("title ASC, ");
				query.append("version ASC");

				Query q = session.createQuery(query.toString());

				QueryPos qPos = QueryPos.getInstance(q);

				qPos.add(nodeId);

				if (title != null) {
					qPos.add(title);
				}

				List<WikiPage> list = q.list();

				FinderCacheUtil.putResult(finderClassNameCacheEnabled,
					finderClassName, finderMethodName, finderParams,
					finderArgs, list);

				return list;
			}
			catch (Exception e) {
				throw processException(e);
			}
			finally {
				closeSession(session);
			}
		}
		else {
			return (List<WikiPage>)result;
		}
	}

	public List<WikiPage> findByN_T(long nodeId, String title, int start,
		int end) throws SystemException {
		return findByN_T(nodeId, title, start, end, null);
	}

	public List<WikiPage> findByN_T(long nodeId, String title, int start,
		int end, OrderByComparator obc) throws SystemException {
		boolean finderClassNameCacheEnabled = WikiPageModelImpl.CACHE_ENABLED;
		String finderClassName = WikiPage.class.getName();
		String finderMethodName = "findByN_T";
		String[] finderParams = new String[] {
				Long.class.getName(), String.class.getName(),
				
				"java.lang.Integer", "java.lang.Integer",
				"com.liferay.portal.kernel.util.OrderByComparator"
			};
		Object[] finderArgs = new Object[] {
				new Long(nodeId),
				
				title,
				
				String.valueOf(start), String.valueOf(end), String.valueOf(obc)
			};

		Object result = null;

		if (finderClassNameCacheEnabled) {
			result = FinderCacheUtil.getResult(finderClassName,
					finderMethodName, finderParams, finderArgs, this);
		}

		if (result == null) {
			Session session = null;

			try {
				session = openSession();

				StringBuilder query = new StringBuilder();

				query.append(
					"FROM com.liferay.portlet.wiki.model.WikiPage WHERE ");

				query.append("nodeId = ?");

				query.append(" AND ");

				if (title == null) {
					query.append("title IS NULL");
				}
				else {
					query.append("title = ?");
				}

				query.append(" ");

				if (obc != null) {
					query.append("ORDER BY ");
					query.append(obc.getOrderBy());
				}

				else {
					query.append("ORDER BY ");

					query.append("nodeId ASC, ");
					query.append("title ASC, ");
					query.append("version ASC");
				}

				Query q = session.createQuery(query.toString());

				QueryPos qPos = QueryPos.getInstance(q);

				qPos.add(nodeId);

				if (title != null) {
					qPos.add(title);
				}

				List<WikiPage> list = (List<WikiPage>)QueryUtil.list(q,
						getDialect(), start, end);

				FinderCacheUtil.putResult(finderClassNameCacheEnabled,
					finderClassName, finderMethodName, finderParams,
					finderArgs, list);

				return list;
			}
			catch (Exception e) {
				throw processException(e);
			}
			finally {
				closeSession(session);
			}
		}
		else {
			return (List<WikiPage>)result;
		}
	}

	public WikiPage findByN_T_First(long nodeId, String title,
		OrderByComparator obc) throws NoSuchPageException, SystemException {
		List<WikiPage> list = findByN_T(nodeId, title, 0, 1, obc);

		if (list.size() == 0) {
			StringBuilder msg = new StringBuilder();

			msg.append("No WikiPage exists with the key {");

			msg.append("nodeId=" + nodeId);

			msg.append(", ");
			msg.append("title=" + title);

			msg.append(StringPool.CLOSE_CURLY_BRACE);

			throw new NoSuchPageException(msg.toString());
		}
		else {
			return list.get(0);
		}
	}

	public WikiPage findByN_T_Last(long nodeId, String title,
		OrderByComparator obc) throws NoSuchPageException, SystemException {
		int count = countByN_T(nodeId, title);

		List<WikiPage> list = findByN_T(nodeId, title, count - 1, count, obc);

		if (list.size() == 0) {
			StringBuilder msg = new StringBuilder();

			msg.append("No WikiPage exists with the key {");

			msg.append("nodeId=" + nodeId);

			msg.append(", ");
			msg.append("title=" + title);

			msg.append(StringPool.CLOSE_CURLY_BRACE);

			throw new NoSuchPageException(msg.toString());
		}
		else {
			return list.get(0);
		}
	}

	public WikiPage[] findByN_T_PrevAndNext(long pageId, long nodeId,
		String title, OrderByComparator obc)
		throws NoSuchPageException, SystemException {
		WikiPage wikiPage = findByPrimaryKey(pageId);

		int count = countByN_T(nodeId, title);

		Session session = null;

		try {
			session = openSession();

			StringBuilder query = new StringBuilder();

			query.append("FROM com.liferay.portlet.wiki.model.WikiPage WHERE ");

			query.append("nodeId = ?");

			query.append(" AND ");

			if (title == null) {
				query.append("title IS NULL");
			}
			else {
				query.append("title = ?");
			}

			query.append(" ");

			if (obc != null) {
				query.append("ORDER BY ");
				query.append(obc.getOrderBy());
			}

			else {
				query.append("ORDER BY ");

				query.append("nodeId ASC, ");
				query.append("title ASC, ");
				query.append("version ASC");
			}

			Query q = session.createQuery(query.toString());

			QueryPos qPos = QueryPos.getInstance(q);

			qPos.add(nodeId);

			if (title != null) {
				qPos.add(title);
			}

			Object[] objArray = QueryUtil.getPrevAndNext(q, count, obc, wikiPage);

			WikiPage[] array = new WikiPageImpl[3];

			array[0] = (WikiPage)objArray[0];
			array[1] = (WikiPage)objArray[1];
			array[2] = (WikiPage)objArray[2];

			return array;
		}
		catch (Exception e) {
			throw processException(e);
		}
		finally {
			closeSession(session);
		}
	}

	public List<WikiPage> findByN_H(long nodeId, boolean head)
		throws SystemException {
		boolean finderClassNameCacheEnabled = WikiPageModelImpl.CACHE_ENABLED;
		String finderClassName = WikiPage.class.getName();
		String finderMethodName = "findByN_H";
		String[] finderParams = new String[] {
				Long.class.getName(), Boolean.class.getName()
			};
		Object[] finderArgs = new Object[] {
				new Long(nodeId), Boolean.valueOf(head)
			};

		Object result = null;

		if (finderClassNameCacheEnabled) {
			result = FinderCacheUtil.getResult(finderClassName,
					finderMethodName, finderParams, finderArgs, this);
		}

		if (result == null) {
			Session session = null;

			try {
				session = openSession();

				StringBuilder query = new StringBuilder();

				query.append(
					"FROM com.liferay.portlet.wiki.model.WikiPage WHERE ");

				query.append("nodeId = ?");

				query.append(" AND ");

				query.append("head = ?");

				query.append(" ");

				query.append("ORDER BY ");

				query.append("nodeId ASC, ");
				query.append("title ASC, ");
				query.append("version ASC");

				Query q = session.createQuery(query.toString());

				QueryPos qPos = QueryPos.getInstance(q);

				qPos.add(nodeId);

				qPos.add(head);

				List<WikiPage> list = q.list();

				FinderCacheUtil.putResult(finderClassNameCacheEnabled,
					finderClassName, finderMethodName, finderParams,
					finderArgs, list);

				return list;
			}
			catch (Exception e) {
				throw processException(e);
			}
			finally {
				closeSession(session);
			}
		}
		else {
			return (List<WikiPage>)result;
		}
	}

	public List<WikiPage> findByN_H(long nodeId, boolean head, int start,
		int end) throws SystemException {
		return findByN_H(nodeId, head, start, end, null);
	}

	public List<WikiPage> findByN_H(long nodeId, boolean head, int start,
		int end, OrderByComparator obc) throws SystemException {
		boolean finderClassNameCacheEnabled = WikiPageModelImpl.CACHE_ENABLED;
		String finderClassName = WikiPage.class.getName();
		String finderMethodName = "findByN_H";
		String[] finderParams = new String[] {
				Long.class.getName(), Boolean.class.getName(),
				
				"java.lang.Integer", "java.lang.Integer",
				"com.liferay.portal.kernel.util.OrderByComparator"
			};
		Object[] finderArgs = new Object[] {
				new Long(nodeId), Boolean.valueOf(head),
				
				String.valueOf(start), String.valueOf(end), String.valueOf(obc)
			};

		Object result = null;

		if (finderClassNameCacheEnabled) {
			result = FinderCacheUtil.getResult(finderClassName,
					finderMethodName, finderParams, finderArgs, this);
		}

		if (result == null) {
			Session session = null;

			try {
				session = openSession();

				StringBuilder query = new StringBuilder();

				query.append(
					"FROM com.liferay.portlet.wiki.model.WikiPage WHERE ");

				query.append("nodeId = ?");

				query.append(" AND ");

				query.append("head = ?");

				query.append(" ");

				if (obc != null) {
					query.append("ORDER BY ");
					query.append(obc.getOrderBy());
				}

				else {
					query.append("ORDER BY ");

					query.append("nodeId ASC, ");
					query.append("title ASC, ");
					query.append("version ASC");
				}

				Query q = session.createQuery(query.toString());

				QueryPos qPos = QueryPos.getInstance(q);

				qPos.add(nodeId);

				qPos.add(head);

				List<WikiPage> list = (List<WikiPage>)QueryUtil.list(q,
						getDialect(), start, end);

				FinderCacheUtil.putResult(finderClassNameCacheEnabled,
					finderClassName, finderMethodName, finderParams,
					finderArgs, list);

				return list;
			}
			catch (Exception e) {
				throw processException(e);
			}
			finally {
				closeSession(session);
			}
		}
		else {
			return (List<WikiPage>)result;
		}
	}

	public WikiPage findByN_H_First(long nodeId, boolean head,
		OrderByComparator obc) throws NoSuchPageException, SystemException {
		List<WikiPage> list = findByN_H(nodeId, head, 0, 1, obc);

		if (list.size() == 0) {
			StringBuilder msg = new StringBuilder();

			msg.append("No WikiPage exists with the key {");

			msg.append("nodeId=" + nodeId);

			msg.append(", ");
			msg.append("head=" + head);

			msg.append(StringPool.CLOSE_CURLY_BRACE);

			throw new NoSuchPageException(msg.toString());
		}
		else {
			return list.get(0);
		}
	}

	public WikiPage findByN_H_Last(long nodeId, boolean head,
		OrderByComparator obc) throws NoSuchPageException, SystemException {
		int count = countByN_H(nodeId, head);

		List<WikiPage> list = findByN_H(nodeId, head, count - 1, count, obc);

		if (list.size() == 0) {
			StringBuilder msg = new StringBuilder();

			msg.append("No WikiPage exists with the key {");

			msg.append("nodeId=" + nodeId);

			msg.append(", ");
			msg.append("head=" + head);

			msg.append(StringPool.CLOSE_CURLY_BRACE);

			throw new NoSuchPageException(msg.toString());
		}
		else {
			return list.get(0);
		}
	}

	public WikiPage[] findByN_H_PrevAndNext(long pageId, long nodeId,
		boolean head, OrderByComparator obc)
		throws NoSuchPageException, SystemException {
		WikiPage wikiPage = findByPrimaryKey(pageId);

		int count = countByN_H(nodeId, head);

		Session session = null;

		try {
			session = openSession();

			StringBuilder query = new StringBuilder();

			query.append("FROM com.liferay.portlet.wiki.model.WikiPage WHERE ");

			query.append("nodeId = ?");

			query.append(" AND ");

			query.append("head = ?");

			query.append(" ");

			if (obc != null) {
				query.append("ORDER BY ");
				query.append(obc.getOrderBy());
			}

			else {
				query.append("ORDER BY ");

				query.append("nodeId ASC, ");
				query.append("title ASC, ");
				query.append("version ASC");
			}

			Query q = session.createQuery(query.toString());

			QueryPos qPos = QueryPos.getInstance(q);

			qPos.add(nodeId);

			qPos.add(head);

			Object[] objArray = QueryUtil.getPrevAndNext(q, count, obc, wikiPage);

			WikiPage[] array = new WikiPageImpl[3];

			array[0] = (WikiPage)objArray[0];
			array[1] = (WikiPage)objArray[1];
			array[2] = (WikiPage)objArray[2];

			return array;
		}
		catch (Exception e) {
			throw processException(e);
		}
		finally {
			closeSession(session);
		}
	}

	public List<WikiPage> findByN_P(long nodeId, String parentTitle)
		throws SystemException {
		boolean finderClassNameCacheEnabled = WikiPageModelImpl.CACHE_ENABLED;
		String finderClassName = WikiPage.class.getName();
		String finderMethodName = "findByN_P";
		String[] finderParams = new String[] {
				Long.class.getName(), String.class.getName()
			};
		Object[] finderArgs = new Object[] { new Long(nodeId), parentTitle };

		Object result = null;

		if (finderClassNameCacheEnabled) {
			result = FinderCacheUtil.getResult(finderClassName,
					finderMethodName, finderParams, finderArgs, this);
		}

		if (result == null) {
			Session session = null;

			try {
				session = openSession();

				StringBuilder query = new StringBuilder();

				query.append(
					"FROM com.liferay.portlet.wiki.model.WikiPage WHERE ");

				query.append("nodeId = ?");

				query.append(" AND ");

				if (parentTitle == null) {
					query.append("parentTitle IS NULL");
				}
				else {
					query.append("parentTitle = ?");
				}

				query.append(" ");

				query.append("ORDER BY ");

				query.append("nodeId ASC, ");
				query.append("title ASC, ");
				query.append("version ASC");

				Query q = session.createQuery(query.toString());

				QueryPos qPos = QueryPos.getInstance(q);

				qPos.add(nodeId);

				if (parentTitle != null) {
					qPos.add(parentTitle);
				}

				List<WikiPage> list = q.list();

				FinderCacheUtil.putResult(finderClassNameCacheEnabled,
					finderClassName, finderMethodName, finderParams,
					finderArgs, list);

				return list;
			}
			catch (Exception e) {
				throw processException(e);
			}
			finally {
				closeSession(session);
			}
		}
		else {
			return (List<WikiPage>)result;
		}
	}

	public List<WikiPage> findByN_P(long nodeId, String parentTitle, int start,
		int end) throws SystemException {
		return findByN_P(nodeId, parentTitle, start, end, null);
	}

	public List<WikiPage> findByN_P(long nodeId, String parentTitle, int start,
		int end, OrderByComparator obc) throws SystemException {
		boolean finderClassNameCacheEnabled = WikiPageModelImpl.CACHE_ENABLED;
		String finderClassName = WikiPage.class.getName();
		String finderMethodName = "findByN_P";
		String[] finderParams = new String[] {
				Long.class.getName(), String.class.getName(),
				
				"java.lang.Integer", "java.lang.Integer",
				"com.liferay.portal.kernel.util.OrderByComparator"
			};
		Object[] finderArgs = new Object[] {
				new Long(nodeId),
				
				parentTitle,
				
				String.valueOf(start), String.valueOf(end), String.valueOf(obc)
			};

		Object result = null;

		if (finderClassNameCacheEnabled) {
			result = FinderCacheUtil.getResult(finderClassName,
					finderMethodName, finderParams, finderArgs, this);
		}

		if (result == null) {
			Session session = null;

			try {
				session = openSession();

				StringBuilder query = new StringBuilder();

				query.append(
					"FROM com.liferay.portlet.wiki.model.WikiPage WHERE ");

				query.append("nodeId = ?");

				query.append(" AND ");

				if (parentTitle == null) {
					query.append("parentTitle IS NULL");
				}
				else {
					query.append("parentTitle = ?");
				}

				query.append(" ");

				if (obc != null) {
					query.append("ORDER BY ");
					query.append(obc.getOrderBy());
				}

				else {
					query.append("ORDER BY ");

					query.append("nodeId ASC, ");
					query.append("title ASC, ");
					query.append("version ASC");
				}

				Query q = session.createQuery(query.toString());

				QueryPos qPos = QueryPos.getInstance(q);

				qPos.add(nodeId);

				if (parentTitle != null) {
					qPos.add(parentTitle);
				}

				List<WikiPage> list = (List<WikiPage>)QueryUtil.list(q,
						getDialect(), start, end);

				FinderCacheUtil.putResult(finderClassNameCacheEnabled,
					finderClassName, finderMethodName, finderParams,
					finderArgs, list);

				return list;
			}
			catch (Exception e) {
				throw processException(e);
			}
			finally {
				closeSession(session);
			}
		}
		else {
			return (List<WikiPage>)result;
		}
	}

	public WikiPage findByN_P_First(long nodeId, String parentTitle,
		OrderByComparator obc) throws NoSuchPageException, SystemException {
		List<WikiPage> list = findByN_P(nodeId, parentTitle, 0, 1, obc);

		if (list.size() == 0) {
			StringBuilder msg = new StringBuilder();

			msg.append("No WikiPage exists with the key {");

			msg.append("nodeId=" + nodeId);

			msg.append(", ");
			msg.append("parentTitle=" + parentTitle);

			msg.append(StringPool.CLOSE_CURLY_BRACE);

			throw new NoSuchPageException(msg.toString());
		}
		else {
			return list.get(0);
		}
	}

	public WikiPage findByN_P_Last(long nodeId, String parentTitle,
		OrderByComparator obc) throws NoSuchPageException, SystemException {
		int count = countByN_P(nodeId, parentTitle);

		List<WikiPage> list = findByN_P(nodeId, parentTitle, count - 1, count,
				obc);

		if (list.size() == 0) {
			StringBuilder msg = new StringBuilder();

			msg.append("No WikiPage exists with the key {");

			msg.append("nodeId=" + nodeId);

			msg.append(", ");
			msg.append("parentTitle=" + parentTitle);

			msg.append(StringPool.CLOSE_CURLY_BRACE);

			throw new NoSuchPageException(msg.toString());
		}
		else {
			return list.get(0);
		}
	}

	public WikiPage[] findByN_P_PrevAndNext(long pageId, long nodeId,
		String parentTitle, OrderByComparator obc)
		throws NoSuchPageException, SystemException {
		WikiPage wikiPage = findByPrimaryKey(pageId);

		int count = countByN_P(nodeId, parentTitle);

		Session session = null;

		try {
			session = openSession();

			StringBuilder query = new StringBuilder();

			query.append("FROM com.liferay.portlet.wiki.model.WikiPage WHERE ");

			query.append("nodeId = ?");

			query.append(" AND ");

			if (parentTitle == null) {
				query.append("parentTitle IS NULL");
			}
			else {
				query.append("parentTitle = ?");
			}

			query.append(" ");

			if (obc != null) {
				query.append("ORDER BY ");
				query.append(obc.getOrderBy());
			}

			else {
				query.append("ORDER BY ");

				query.append("nodeId ASC, ");
				query.append("title ASC, ");
				query.append("version ASC");
			}

			Query q = session.createQuery(query.toString());

			QueryPos qPos = QueryPos.getInstance(q);

			qPos.add(nodeId);

			if (parentTitle != null) {
				qPos.add(parentTitle);
			}

			Object[] objArray = QueryUtil.getPrevAndNext(q, count, obc, wikiPage);

			WikiPage[] array = new WikiPageImpl[3];

			array[0] = (WikiPage)objArray[0];
			array[1] = (WikiPage)objArray[1];
			array[2] = (WikiPage)objArray[2];

			return array;
		}
		catch (Exception e) {
			throw processException(e);
		}
		finally {
			closeSession(session);
		}
	}

	public List<WikiPage> findByN_R(long nodeId, String redirectTitle)
		throws SystemException {
		boolean finderClassNameCacheEnabled = WikiPageModelImpl.CACHE_ENABLED;
		String finderClassName = WikiPage.class.getName();
		String finderMethodName = "findByN_R";
		String[] finderParams = new String[] {
				Long.class.getName(), String.class.getName()
			};
		Object[] finderArgs = new Object[] { new Long(nodeId), redirectTitle };

		Object result = null;

		if (finderClassNameCacheEnabled) {
			result = FinderCacheUtil.getResult(finderClassName,
					finderMethodName, finderParams, finderArgs, this);
		}

		if (result == null) {
			Session session = null;

			try {
				session = openSession();

				StringBuilder query = new StringBuilder();

				query.append(
					"FROM com.liferay.portlet.wiki.model.WikiPage WHERE ");

				query.append("nodeId = ?");

				query.append(" AND ");

				if (redirectTitle == null) {
					query.append("redirectTitle IS NULL");
				}
				else {
					query.append("redirectTitle = ?");
				}

				query.append(" ");

				query.append("ORDER BY ");

				query.append("nodeId ASC, ");
				query.append("title ASC, ");
				query.append("version ASC");

				Query q = session.createQuery(query.toString());

				QueryPos qPos = QueryPos.getInstance(q);

				qPos.add(nodeId);

				if (redirectTitle != null) {
					qPos.add(redirectTitle);
				}

				List<WikiPage> list = q.list();

				FinderCacheUtil.putResult(finderClassNameCacheEnabled,
					finderClassName, finderMethodName, finderParams,
					finderArgs, list);

				return list;
			}
			catch (Exception e) {
				throw processException(e);
			}
			finally {
				closeSession(session);
			}
		}
		else {
			return (List<WikiPage>)result;
		}
	}

	public List<WikiPage> findByN_R(long nodeId, String redirectTitle,
		int start, int end) throws SystemException {
		return findByN_R(nodeId, redirectTitle, start, end, null);
	}

	public List<WikiPage> findByN_R(long nodeId, String redirectTitle,
		int start, int end, OrderByComparator obc) throws SystemException {
		boolean finderClassNameCacheEnabled = WikiPageModelImpl.CACHE_ENABLED;
		String finderClassName = WikiPage.class.getName();
		String finderMethodName = "findByN_R";
		String[] finderParams = new String[] {
				Long.class.getName(), String.class.getName(),
				
				"java.lang.Integer", "java.lang.Integer",
				"com.liferay.portal.kernel.util.OrderByComparator"
			};
		Object[] finderArgs = new Object[] {
				new Long(nodeId),
				
				redirectTitle,
				
				String.valueOf(start), String.valueOf(end), String.valueOf(obc)
			};

		Object result = null;

		if (finderClassNameCacheEnabled) {
			result = FinderCacheUtil.getResult(finderClassName,
					finderMethodName, finderParams, finderArgs, this);
		}

		if (result == null) {
			Session session = null;

			try {
				session = openSession();

				StringBuilder query = new StringBuilder();

				query.append(
					"FROM com.liferay.portlet.wiki.model.WikiPage WHERE ");

				query.append("nodeId = ?");

				query.append(" AND ");

				if (redirectTitle == null) {
					query.append("redirectTitle IS NULL");
				}
				else {
					query.append("redirectTitle = ?");
				}

				query.append(" ");

				if (obc != null) {
					query.append("ORDER BY ");
					query.append(obc.getOrderBy());
				}

				else {
					query.append("ORDER BY ");

					query.append("nodeId ASC, ");
					query.append("title ASC, ");
					query.append("version ASC");
				}

				Query q = session.createQuery(query.toString());

				QueryPos qPos = QueryPos.getInstance(q);

				qPos.add(nodeId);

				if (redirectTitle != null) {
					qPos.add(redirectTitle);
				}

				List<WikiPage> list = (List<WikiPage>)QueryUtil.list(q,
						getDialect(), start, end);

				FinderCacheUtil.putResult(finderClassNameCacheEnabled,
					finderClassName, finderMethodName, finderParams,
					finderArgs, list);

				return list;
			}
			catch (Exception e) {
				throw processException(e);
			}
			finally {
				closeSession(session);
			}
		}
		else {
			return (List<WikiPage>)result;
		}
	}

	public WikiPage findByN_R_First(long nodeId, String redirectTitle,
		OrderByComparator obc) throws NoSuchPageException, SystemException {
		List<WikiPage> list = findByN_R(nodeId, redirectTitle, 0, 1, obc);

		if (list.size() == 0) {
			StringBuilder msg = new StringBuilder();

			msg.append("No WikiPage exists with the key {");

			msg.append("nodeId=" + nodeId);

			msg.append(", ");
			msg.append("redirectTitle=" + redirectTitle);

			msg.append(StringPool.CLOSE_CURLY_BRACE);

			throw new NoSuchPageException(msg.toString());
		}
		else {
			return list.get(0);
		}
	}

	public WikiPage findByN_R_Last(long nodeId, String redirectTitle,
		OrderByComparator obc) throws NoSuchPageException, SystemException {
		int count = countByN_R(nodeId, redirectTitle);

		List<WikiPage> list = findByN_R(nodeId, redirectTitle, count - 1,
				count, obc);

		if (list.size() == 0) {
			StringBuilder msg = new StringBuilder();

			msg.append("No WikiPage exists with the key {");

			msg.append("nodeId=" + nodeId);

			msg.append(", ");
			msg.append("redirectTitle=" + redirectTitle);

			msg.append(StringPool.CLOSE_CURLY_BRACE);

			throw new NoSuchPageException(msg.toString());
		}
		else {
			return list.get(0);
		}
	}

	public WikiPage[] findByN_R_PrevAndNext(long pageId, long nodeId,
		String redirectTitle, OrderByComparator obc)
		throws NoSuchPageException, SystemException {
		WikiPage wikiPage = findByPrimaryKey(pageId);

		int count = countByN_R(nodeId, redirectTitle);

		Session session = null;

		try {
			session = openSession();

			StringBuilder query = new StringBuilder();

			query.append("FROM com.liferay.portlet.wiki.model.WikiPage WHERE ");

			query.append("nodeId = ?");

			query.append(" AND ");

			if (redirectTitle == null) {
				query.append("redirectTitle IS NULL");
			}
			else {
				query.append("redirectTitle = ?");
			}

			query.append(" ");

			if (obc != null) {
				query.append("ORDER BY ");
				query.append(obc.getOrderBy());
			}

			else {
				query.append("ORDER BY ");

				query.append("nodeId ASC, ");
				query.append("title ASC, ");
				query.append("version ASC");
			}

			Query q = session.createQuery(query.toString());

			QueryPos qPos = QueryPos.getInstance(q);

			qPos.add(nodeId);

			if (redirectTitle != null) {
				qPos.add(redirectTitle);
			}

			Object[] objArray = QueryUtil.getPrevAndNext(q, count, obc, wikiPage);

			WikiPage[] array = new WikiPageImpl[3];

			array[0] = (WikiPage)objArray[0];
			array[1] = (WikiPage)objArray[1];
			array[2] = (WikiPage)objArray[2];

			return array;
		}
		catch (Exception e) {
			throw processException(e);
		}
		finally {
			closeSession(session);
		}
	}

	public WikiPage findByN_T_V(long nodeId, String title, double version)
		throws NoSuchPageException, SystemException {
		WikiPage wikiPage = fetchByN_T_V(nodeId, title, version);

		if (wikiPage == null) {
			StringBuilder msg = new StringBuilder();

			msg.append("No WikiPage exists with the key {");

			msg.append("nodeId=" + nodeId);

			msg.append(", ");
			msg.append("title=" + title);

			msg.append(", ");
			msg.append("version=" + version);

			msg.append(StringPool.CLOSE_CURLY_BRACE);

			if (_log.isWarnEnabled()) {
				_log.warn(msg.toString());
			}

			throw new NoSuchPageException(msg.toString());
		}

		return wikiPage;
	}

	public WikiPage fetchByN_T_V(long nodeId, String title, double version)
		throws SystemException {
		boolean finderClassNameCacheEnabled = WikiPageModelImpl.CACHE_ENABLED;
		String finderClassName = WikiPage.class.getName();
		String finderMethodName = "fetchByN_T_V";
		String[] finderParams = new String[] {
				Long.class.getName(), String.class.getName(),
				Double.class.getName()
			};
		Object[] finderArgs = new Object[] {
				new Long(nodeId),
				
				title, new Double(version)
			};

		Object result = null;

		if (finderClassNameCacheEnabled) {
			result = FinderCacheUtil.getResult(finderClassName,
					finderMethodName, finderParams, finderArgs, this);
		}

		if (result == null) {
			Session session = null;

			try {
				session = openSession();

				StringBuilder query = new StringBuilder();

				query.append(
					"FROM com.liferay.portlet.wiki.model.WikiPage WHERE ");

				query.append("nodeId = ?");

				query.append(" AND ");

				if (title == null) {
					query.append("title IS NULL");
				}
				else {
					query.append("title = ?");
				}

				query.append(" AND ");

				query.append("version = ?");

				query.append(" ");

				query.append("ORDER BY ");

				query.append("nodeId ASC, ");
				query.append("title ASC, ");
				query.append("version ASC");

				Query q = session.createQuery(query.toString());

				QueryPos qPos = QueryPos.getInstance(q);

				qPos.add(nodeId);

				if (title != null) {
					qPos.add(title);
				}

				qPos.add(version);

				List<WikiPage> list = q.list();

				FinderCacheUtil.putResult(finderClassNameCacheEnabled,
					finderClassName, finderMethodName, finderParams,
					finderArgs, list);

				if (list.size() == 0) {
					return null;
				}
				else {
					return list.get(0);
				}
			}
			catch (Exception e) {
				throw processException(e);
			}
			finally {
				closeSession(session);
			}
		}
		else {
			List<WikiPage> list = (List<WikiPage>)result;

			if (list.size() == 0) {
				return null;
			}
			else {
				return list.get(0);
			}
		}
	}

	public List<WikiPage> findByN_T_H(long nodeId, String title, boolean head)
		throws SystemException {
		boolean finderClassNameCacheEnabled = WikiPageModelImpl.CACHE_ENABLED;
		String finderClassName = WikiPage.class.getName();
		String finderMethodName = "findByN_T_H";
		String[] finderParams = new String[] {
				Long.class.getName(), String.class.getName(),
				Boolean.class.getName()
			};
		Object[] finderArgs = new Object[] {
				new Long(nodeId),
				
				title, Boolean.valueOf(head)
			};

		Object result = null;

		if (finderClassNameCacheEnabled) {
			result = FinderCacheUtil.getResult(finderClassName,
					finderMethodName, finderParams, finderArgs, this);
		}

		if (result == null) {
			Session session = null;

			try {
				session = openSession();

				StringBuilder query = new StringBuilder();

				query.append(
					"FROM com.liferay.portlet.wiki.model.WikiPage WHERE ");

				query.append("nodeId = ?");

				query.append(" AND ");

				if (title == null) {
					query.append("title IS NULL");
				}
				else {
					query.append("title = ?");
				}

				query.append(" AND ");

				query.append("head = ?");

				query.append(" ");

				query.append("ORDER BY ");

				query.append("nodeId ASC, ");
				query.append("title ASC, ");
				query.append("version ASC");

				Query q = session.createQuery(query.toString());

				QueryPos qPos = QueryPos.getInstance(q);

				qPos.add(nodeId);

				if (title != null) {
					qPos.add(title);
				}

				qPos.add(head);

				List<WikiPage> list = q.list();

				FinderCacheUtil.putResult(finderClassNameCacheEnabled,
					finderClassName, finderMethodName, finderParams,
					finderArgs, list);

				return list;
			}
			catch (Exception e) {
				throw processException(e);
			}
			finally {
				closeSession(session);
			}
		}
		else {
			return (List<WikiPage>)result;
		}
	}

	public List<WikiPage> findByN_T_H(long nodeId, String title, boolean head,
		int start, int end) throws SystemException {
		return findByN_T_H(nodeId, title, head, start, end, null);
	}

	public List<WikiPage> findByN_T_H(long nodeId, String title, boolean head,
		int start, int end, OrderByComparator obc) throws SystemException {
		boolean finderClassNameCacheEnabled = WikiPageModelImpl.CACHE_ENABLED;
		String finderClassName = WikiPage.class.getName();
		String finderMethodName = "findByN_T_H";
		String[] finderParams = new String[] {
				Long.class.getName(), String.class.getName(),
				Boolean.class.getName(),
				
				"java.lang.Integer", "java.lang.Integer",
				"com.liferay.portal.kernel.util.OrderByComparator"
			};
		Object[] finderArgs = new Object[] {
				new Long(nodeId),
				
				title, Boolean.valueOf(head),
				
				String.valueOf(start), String.valueOf(end), String.valueOf(obc)
			};

		Object result = null;

		if (finderClassNameCacheEnabled) {
			result = FinderCacheUtil.getResult(finderClassName,
					finderMethodName, finderParams, finderArgs, this);
		}

		if (result == null) {
			Session session = null;

			try {
				session = openSession();

				StringBuilder query = new StringBuilder();

				query.append(
					"FROM com.liferay.portlet.wiki.model.WikiPage WHERE ");

				query.append("nodeId = ?");

				query.append(" AND ");

				if (title == null) {
					query.append("title IS NULL");
				}
				else {
					query.append("title = ?");
				}

				query.append(" AND ");

				query.append("head = ?");

				query.append(" ");

				if (obc != null) {
					query.append("ORDER BY ");
					query.append(obc.getOrderBy());
				}

				else {
					query.append("ORDER BY ");

					query.append("nodeId ASC, ");
					query.append("title ASC, ");
					query.append("version ASC");
				}

				Query q = session.createQuery(query.toString());

				QueryPos qPos = QueryPos.getInstance(q);

				qPos.add(nodeId);

				if (title != null) {
					qPos.add(title);
				}

				qPos.add(head);

				List<WikiPage> list = (List<WikiPage>)QueryUtil.list(q,
						getDialect(), start, end);

				FinderCacheUtil.putResult(finderClassNameCacheEnabled,
					finderClassName, finderMethodName, finderParams,
					finderArgs, list);

				return list;
			}
			catch (Exception e) {
				throw processException(e);
			}
			finally {
				closeSession(session);
			}
		}
		else {
			return (List<WikiPage>)result;
		}
	}

	public WikiPage findByN_T_H_First(long nodeId, String title, boolean head,
		OrderByComparator obc) throws NoSuchPageException, SystemException {
		List<WikiPage> list = findByN_T_H(nodeId, title, head, 0, 1, obc);

		if (list.size() == 0) {
			StringBuilder msg = new StringBuilder();

			msg.append("No WikiPage exists with the key {");

			msg.append("nodeId=" + nodeId);

			msg.append(", ");
			msg.append("title=" + title);

			msg.append(", ");
			msg.append("head=" + head);

			msg.append(StringPool.CLOSE_CURLY_BRACE);

			throw new NoSuchPageException(msg.toString());
		}
		else {
			return list.get(0);
		}
	}

	public WikiPage findByN_T_H_Last(long nodeId, String title, boolean head,
		OrderByComparator obc) throws NoSuchPageException, SystemException {
		int count = countByN_T_H(nodeId, title, head);

		List<WikiPage> list = findByN_T_H(nodeId, title, head, count - 1,
				count, obc);

		if (list.size() == 0) {
			StringBuilder msg = new StringBuilder();

			msg.append("No WikiPage exists with the key {");

			msg.append("nodeId=" + nodeId);

			msg.append(", ");
			msg.append("title=" + title);

			msg.append(", ");
			msg.append("head=" + head);

			msg.append(StringPool.CLOSE_CURLY_BRACE);

			throw new NoSuchPageException(msg.toString());
		}
		else {
			return list.get(0);
		}
	}

	public WikiPage[] findByN_T_H_PrevAndNext(long pageId, long nodeId,
		String title, boolean head, OrderByComparator obc)
		throws NoSuchPageException, SystemException {
		WikiPage wikiPage = findByPrimaryKey(pageId);

		int count = countByN_T_H(nodeId, title, head);

		Session session = null;

		try {
			session = openSession();

			StringBuilder query = new StringBuilder();

			query.append("FROM com.liferay.portlet.wiki.model.WikiPage WHERE ");

			query.append("nodeId = ?");

			query.append(" AND ");

			if (title == null) {
				query.append("title IS NULL");
			}
			else {
				query.append("title = ?");
			}

			query.append(" AND ");

			query.append("head = ?");

			query.append(" ");

			if (obc != null) {
				query.append("ORDER BY ");
				query.append(obc.getOrderBy());
			}

			else {
				query.append("ORDER BY ");

				query.append("nodeId ASC, ");
				query.append("title ASC, ");
				query.append("version ASC");
			}

			Query q = session.createQuery(query.toString());

			QueryPos qPos = QueryPos.getInstance(q);

			qPos.add(nodeId);

			if (title != null) {
				qPos.add(title);
			}

			qPos.add(head);

			Object[] objArray = QueryUtil.getPrevAndNext(q, count, obc, wikiPage);

			WikiPage[] array = new WikiPageImpl[3];

			array[0] = (WikiPage)objArray[0];
			array[1] = (WikiPage)objArray[1];
			array[2] = (WikiPage)objArray[2];

			return array;
		}
		catch (Exception e) {
			throw processException(e);
		}
		finally {
			closeSession(session);
		}
	}

	public List<WikiPage> findByN_H_P(long nodeId, boolean head,
		String parentTitle) throws SystemException {
		boolean finderClassNameCacheEnabled = WikiPageModelImpl.CACHE_ENABLED;
		String finderClassName = WikiPage.class.getName();
		String finderMethodName = "findByN_H_P";
		String[] finderParams = new String[] {
				Long.class.getName(), Boolean.class.getName(),
				String.class.getName()
			};
		Object[] finderArgs = new Object[] {
				new Long(nodeId), Boolean.valueOf(head),
				
				parentTitle
			};

		Object result = null;

		if (finderClassNameCacheEnabled) {
			result = FinderCacheUtil.getResult(finderClassName,
					finderMethodName, finderParams, finderArgs, this);
		}

		if (result == null) {
			Session session = null;

			try {
				session = openSession();

				StringBuilder query = new StringBuilder();

				query.append(
					"FROM com.liferay.portlet.wiki.model.WikiPage WHERE ");

				query.append("nodeId = ?");

				query.append(" AND ");

				query.append("head = ?");

				query.append(" AND ");

				if (parentTitle == null) {
					query.append("parentTitle IS NULL");
				}
				else {
					query.append("parentTitle = ?");
				}

				query.append(" ");

				query.append("ORDER BY ");

				query.append("nodeId ASC, ");
				query.append("title ASC, ");
				query.append("version ASC");

				Query q = session.createQuery(query.toString());

				QueryPos qPos = QueryPos.getInstance(q);

				qPos.add(nodeId);

				qPos.add(head);

				if (parentTitle != null) {
					qPos.add(parentTitle);
				}

				List<WikiPage> list = q.list();

				FinderCacheUtil.putResult(finderClassNameCacheEnabled,
					finderClassName, finderMethodName, finderParams,
					finderArgs, list);

				return list;
			}
			catch (Exception e) {
				throw processException(e);
			}
			finally {
				closeSession(session);
			}
		}
		else {
			return (List<WikiPage>)result;
		}
	}

	public List<WikiPage> findByN_H_P(long nodeId, boolean head,
		String parentTitle, int start, int end) throws SystemException {
		return findByN_H_P(nodeId, head, parentTitle, start, end, null);
	}

	public List<WikiPage> findByN_H_P(long nodeId, boolean head,
		String parentTitle, int start, int end, OrderByComparator obc)
		throws SystemException {
		boolean finderClassNameCacheEnabled = WikiPageModelImpl.CACHE_ENABLED;
		String finderClassName = WikiPage.class.getName();
		String finderMethodName = "findByN_H_P";
		String[] finderParams = new String[] {
				Long.class.getName(), Boolean.class.getName(),
				String.class.getName(),
				
				"java.lang.Integer", "java.lang.Integer",
				"com.liferay.portal.kernel.util.OrderByComparator"
			};
		Object[] finderArgs = new Object[] {
				new Long(nodeId), Boolean.valueOf(head),
				
				parentTitle,
				
				String.valueOf(start), String.valueOf(end), String.valueOf(obc)
			};

		Object result = null;

		if (finderClassNameCacheEnabled) {
			result = FinderCacheUtil.getResult(finderClassName,
					finderMethodName, finderParams, finderArgs, this);
		}

		if (result == null) {
			Session session = null;

			try {
				session = openSession();

				StringBuilder query = new StringBuilder();

				query.append(
					"FROM com.liferay.portlet.wiki.model.WikiPage WHERE ");

				query.append("nodeId = ?");

				query.append(" AND ");

				query.append("head = ?");

				query.append(" AND ");

				if (parentTitle == null) {
					query.append("parentTitle IS NULL");
				}
				else {
					query.append("parentTitle = ?");
				}

				query.append(" ");

				if (obc != null) {
					query.append("ORDER BY ");
					query.append(obc.getOrderBy());
				}

				else {
					query.append("ORDER BY ");

					query.append("nodeId ASC, ");
					query.append("title ASC, ");
					query.append("version ASC");
				}

				Query q = session.createQuery(query.toString());

				QueryPos qPos = QueryPos.getInstance(q);

				qPos.add(nodeId);

				qPos.add(head);

				if (parentTitle != null) {
					qPos.add(parentTitle);
				}

				List<WikiPage> list = (List<WikiPage>)QueryUtil.list(q,
						getDialect(), start, end);

				FinderCacheUtil.putResult(finderClassNameCacheEnabled,
					finderClassName, finderMethodName, finderParams,
					finderArgs, list);

				return list;
			}
			catch (Exception e) {
				throw processException(e);
			}
			finally {
				closeSession(session);
			}
		}
		else {
			return (List<WikiPage>)result;
		}
	}

	public WikiPage findByN_H_P_First(long nodeId, boolean head,
		String parentTitle, OrderByComparator obc)
		throws NoSuchPageException, SystemException {
		List<WikiPage> list = findByN_H_P(nodeId, head, parentTitle, 0, 1, obc);

		if (list.size() == 0) {
			StringBuilder msg = new StringBuilder();

			msg.append("No WikiPage exists with the key {");

			msg.append("nodeId=" + nodeId);

			msg.append(", ");
			msg.append("head=" + head);

			msg.append(", ");
			msg.append("parentTitle=" + parentTitle);

			msg.append(StringPool.CLOSE_CURLY_BRACE);

			throw new NoSuchPageException(msg.toString());
		}
		else {
			return list.get(0);
		}
	}

	public WikiPage findByN_H_P_Last(long nodeId, boolean head,
		String parentTitle, OrderByComparator obc)
		throws NoSuchPageException, SystemException {
		int count = countByN_H_P(nodeId, head, parentTitle);

		List<WikiPage> list = findByN_H_P(nodeId, head, parentTitle, count - 1,
				count, obc);

		if (list.size() == 0) {
			StringBuilder msg = new StringBuilder();

			msg.append("No WikiPage exists with the key {");

			msg.append("nodeId=" + nodeId);

			msg.append(", ");
			msg.append("head=" + head);

			msg.append(", ");
			msg.append("parentTitle=" + parentTitle);

			msg.append(StringPool.CLOSE_CURLY_BRACE);

			throw new NoSuchPageException(msg.toString());
		}
		else {
			return list.get(0);
		}
	}

	public WikiPage[] findByN_H_P_PrevAndNext(long pageId, long nodeId,
		boolean head, String parentTitle, OrderByComparator obc)
		throws NoSuchPageException, SystemException {
		WikiPage wikiPage = findByPrimaryKey(pageId);

		int count = countByN_H_P(nodeId, head, parentTitle);

		Session session = null;

		try {
			session = openSession();

			StringBuilder query = new StringBuilder();

			query.append("FROM com.liferay.portlet.wiki.model.WikiPage WHERE ");

			query.append("nodeId = ?");

			query.append(" AND ");

			query.append("head = ?");

			query.append(" AND ");

			if (parentTitle == null) {
				query.append("parentTitle IS NULL");
			}
			else {
				query.append("parentTitle = ?");
			}

			query.append(" ");

			if (obc != null) {
				query.append("ORDER BY ");
				query.append(obc.getOrderBy());
			}

			else {
				query.append("ORDER BY ");

				query.append("nodeId ASC, ");
				query.append("title ASC, ");
				query.append("version ASC");
			}

			Query q = session.createQuery(query.toString());

			QueryPos qPos = QueryPos.getInstance(q);

			qPos.add(nodeId);

			qPos.add(head);

			if (parentTitle != null) {
				qPos.add(parentTitle);
			}

			Object[] objArray = QueryUtil.getPrevAndNext(q, count, obc, wikiPage);

			WikiPage[] array = new WikiPageImpl[3];

			array[0] = (WikiPage)objArray[0];
			array[1] = (WikiPage)objArray[1];
			array[2] = (WikiPage)objArray[2];

			return array;
		}
		catch (Exception e) {
			throw processException(e);
		}
		finally {
			closeSession(session);
		}
	}

	public List<Object> findWithDynamicQuery(DynamicQuery dynamicQuery)
		throws SystemException {
		Session session = null;

		try {
			session = openSession();

			dynamicQuery.compile(session);

			return dynamicQuery.list();
		}
		catch (Exception e) {
			throw processException(e);
		}
		finally {
			closeSession(session);
		}
	}

	public List<Object> findWithDynamicQuery(DynamicQuery dynamicQuery,
		int start, int end) throws SystemException {
		Session session = null;

		try {
			session = openSession();

			dynamicQuery.setLimit(start, end);

			dynamicQuery.compile(session);

			return dynamicQuery.list();
		}
		catch (Exception e) {
			throw processException(e);
		}
		finally {
			closeSession(session);
		}
	}

	public List<WikiPage> findAll() throws SystemException {
		return findAll(QueryUtil.ALL_POS, QueryUtil.ALL_POS, null);
	}

	public List<WikiPage> findAll(int start, int end) throws SystemException {
		return findAll(start, end, null);
	}

	public List<WikiPage> findAll(int start, int end, OrderByComparator obc)
		throws SystemException {
		boolean finderClassNameCacheEnabled = WikiPageModelImpl.CACHE_ENABLED;
		String finderClassName = WikiPage.class.getName();
		String finderMethodName = "findAll";
		String[] finderParams = new String[] {
				"java.lang.Integer", "java.lang.Integer",
				"com.liferay.portal.kernel.util.OrderByComparator"
			};
		Object[] finderArgs = new Object[] {
				String.valueOf(start), String.valueOf(end), String.valueOf(obc)
			};

		Object result = null;

		if (finderClassNameCacheEnabled) {
			result = FinderCacheUtil.getResult(finderClassName,
					finderMethodName, finderParams, finderArgs, this);
		}

		if (result == null) {
			Session session = null;

			try {
				session = openSession();

				StringBuilder query = new StringBuilder();

				query.append("FROM com.liferay.portlet.wiki.model.WikiPage ");

				if (obc != null) {
					query.append("ORDER BY ");
					query.append(obc.getOrderBy());
				}

				else {
					query.append("ORDER BY ");

					query.append("nodeId ASC, ");
					query.append("title ASC, ");
					query.append("version ASC");
				}

				Query q = session.createQuery(query.toString());

				List<WikiPage> list = (List<WikiPage>)QueryUtil.list(q,
						getDialect(), start, end);

				if (obc == null) {
					Collections.sort(list);
				}

				FinderCacheUtil.putResult(finderClassNameCacheEnabled,
					finderClassName, finderMethodName, finderParams,
					finderArgs, list);

				return list;
			}
			catch (Exception e) {
				throw processException(e);
			}
			finally {
				closeSession(session);
			}
		}
		else {
			return (List<WikiPage>)result;
		}
	}

	public void removeByUuid(String uuid) throws SystemException {
		for (WikiPage wikiPage : findByUuid(uuid)) {
			remove(wikiPage);
		}
	}

	public void removeByNodeId(long nodeId) throws SystemException {
		for (WikiPage wikiPage : findByNodeId(nodeId)) {
			remove(wikiPage);
		}
	}

	public void removeByFormat(String format) throws SystemException {
		for (WikiPage wikiPage : findByFormat(format)) {
			remove(wikiPage);
		}
	}

	public void removeByN_T(long nodeId, String title)
		throws SystemException {
		for (WikiPage wikiPage : findByN_T(nodeId, title)) {
			remove(wikiPage);
		}
	}

	public void removeByN_H(long nodeId, boolean head)
		throws SystemException {
		for (WikiPage wikiPage : findByN_H(nodeId, head)) {
			remove(wikiPage);
		}
	}

	public void removeByN_P(long nodeId, String parentTitle)
		throws SystemException {
		for (WikiPage wikiPage : findByN_P(nodeId, parentTitle)) {
			remove(wikiPage);
		}
	}

	public void removeByN_R(long nodeId, String redirectTitle)
		throws SystemException {
		for (WikiPage wikiPage : findByN_R(nodeId, redirectTitle)) {
			remove(wikiPage);
		}
	}

	public void removeByN_T_V(long nodeId, String title, double version)
		throws NoSuchPageException, SystemException {
		WikiPage wikiPage = findByN_T_V(nodeId, title, version);

		remove(wikiPage);
	}

	public void removeByN_T_H(long nodeId, String title, boolean head)
		throws SystemException {
		for (WikiPage wikiPage : findByN_T_H(nodeId, title, head)) {
			remove(wikiPage);
		}
	}

	public void removeByN_H_P(long nodeId, boolean head, String parentTitle)
		throws SystemException {
		for (WikiPage wikiPage : findByN_H_P(nodeId, head, parentTitle)) {
			remove(wikiPage);
		}
	}

	public void removeAll() throws SystemException {
		for (WikiPage wikiPage : findAll()) {
			remove(wikiPage);
		}
	}

	public int countByUuid(String uuid) throws SystemException {
		boolean finderClassNameCacheEnabled = WikiPageModelImpl.CACHE_ENABLED;
		String finderClassName = WikiPage.class.getName();
		String finderMethodName = "countByUuid";
		String[] finderParams = new String[] { String.class.getName() };
		Object[] finderArgs = new Object[] { uuid };

		Object result = null;

		if (finderClassNameCacheEnabled) {
			result = FinderCacheUtil.getResult(finderClassName,
					finderMethodName, finderParams, finderArgs, this);
		}

		if (result == null) {
			Session session = null;

			try {
				session = openSession();

				StringBuilder query = new StringBuilder();

				query.append("SELECT COUNT(*) ");
				query.append(
					"FROM com.liferay.portlet.wiki.model.WikiPage WHERE ");

				if (uuid == null) {
					query.append("uuid_ IS NULL");
				}
				else {
					query.append("uuid_ = ?");
				}

				query.append(" ");

				Query q = session.createQuery(query.toString());

				QueryPos qPos = QueryPos.getInstance(q);

				if (uuid != null) {
					qPos.add(uuid);
				}

				Long count = null;

				Iterator<Long> itr = q.list().iterator();

				if (itr.hasNext()) {
					count = itr.next();
				}

				if (count == null) {
					count = new Long(0);
				}

				FinderCacheUtil.putResult(finderClassNameCacheEnabled,
					finderClassName, finderMethodName, finderParams,
					finderArgs, count);

				return count.intValue();
			}
			catch (Exception e) {
				throw processException(e);
			}
			finally {
				closeSession(session);
			}
		}
		else {
			return ((Long)result).intValue();
		}
	}

	public int countByNodeId(long nodeId) throws SystemException {
		boolean finderClassNameCacheEnabled = WikiPageModelImpl.CACHE_ENABLED;
		String finderClassName = WikiPage.class.getName();
		String finderMethodName = "countByNodeId";
		String[] finderParams = new String[] { Long.class.getName() };
		Object[] finderArgs = new Object[] { new Long(nodeId) };

		Object result = null;

		if (finderClassNameCacheEnabled) {
			result = FinderCacheUtil.getResult(finderClassName,
					finderMethodName, finderParams, finderArgs, this);
		}

		if (result == null) {
			Session session = null;

			try {
				session = openSession();

				StringBuilder query = new StringBuilder();

				query.append("SELECT COUNT(*) ");
				query.append(
					"FROM com.liferay.portlet.wiki.model.WikiPage WHERE ");

				query.append("nodeId = ?");

				query.append(" ");

				Query q = session.createQuery(query.toString());

				QueryPos qPos = QueryPos.getInstance(q);

				qPos.add(nodeId);

				Long count = null;

				Iterator<Long> itr = q.list().iterator();

				if (itr.hasNext()) {
					count = itr.next();
				}

				if (count == null) {
					count = new Long(0);
				}

				FinderCacheUtil.putResult(finderClassNameCacheEnabled,
					finderClassName, finderMethodName, finderParams,
					finderArgs, count);

				return count.intValue();
			}
			catch (Exception e) {
				throw processException(e);
			}
			finally {
				closeSession(session);
			}
		}
		else {
			return ((Long)result).intValue();
		}
	}

	public int countByFormat(String format) throws SystemException {
		boolean finderClassNameCacheEnabled = WikiPageModelImpl.CACHE_ENABLED;
		String finderClassName = WikiPage.class.getName();
		String finderMethodName = "countByFormat";
		String[] finderParams = new String[] { String.class.getName() };
		Object[] finderArgs = new Object[] { format };

		Object result = null;

		if (finderClassNameCacheEnabled) {
			result = FinderCacheUtil.getResult(finderClassName,
					finderMethodName, finderParams, finderArgs, this);
		}

		if (result == null) {
			Session session = null;

			try {
				session = openSession();

				StringBuilder query = new StringBuilder();

				query.append("SELECT COUNT(*) ");
				query.append(
					"FROM com.liferay.portlet.wiki.model.WikiPage WHERE ");

				if (format == null) {
					query.append("format IS NULL");
				}
				else {
					query.append("format = ?");
				}

				query.append(" ");

				Query q = session.createQuery(query.toString());

				QueryPos qPos = QueryPos.getInstance(q);

				if (format != null) {
					qPos.add(format);
				}

				Long count = null;

				Iterator<Long> itr = q.list().iterator();

				if (itr.hasNext()) {
					count = itr.next();
				}

				if (count == null) {
					count = new Long(0);
				}

				FinderCacheUtil.putResult(finderClassNameCacheEnabled,
					finderClassName, finderMethodName, finderParams,
					finderArgs, count);

				return count.intValue();
			}
			catch (Exception e) {
				throw processException(e);
			}
			finally {
				closeSession(session);
			}
		}
		else {
			return ((Long)result).intValue();
		}
	}

	public int countByN_T(long nodeId, String title) throws SystemException {
		boolean finderClassNameCacheEnabled = WikiPageModelImpl.CACHE_ENABLED;
		String finderClassName = WikiPage.class.getName();
		String finderMethodName = "countByN_T";
		String[] finderParams = new String[] {
				Long.class.getName(), String.class.getName()
			};
		Object[] finderArgs = new Object[] { new Long(nodeId), title };

		Object result = null;

		if (finderClassNameCacheEnabled) {
			result = FinderCacheUtil.getResult(finderClassName,
					finderMethodName, finderParams, finderArgs, this);
		}

		if (result == null) {
			Session session = null;

			try {
				session = openSession();

				StringBuilder query = new StringBuilder();

				query.append("SELECT COUNT(*) ");
				query.append(
					"FROM com.liferay.portlet.wiki.model.WikiPage WHERE ");

				query.append("nodeId = ?");

				query.append(" AND ");

				if (title == null) {
					query.append("title IS NULL");
				}
				else {
					query.append("title = ?");
				}

				query.append(" ");

				Query q = session.createQuery(query.toString());

				QueryPos qPos = QueryPos.getInstance(q);

				qPos.add(nodeId);

				if (title != null) {
					qPos.add(title);
				}

				Long count = null;

				Iterator<Long> itr = q.list().iterator();

				if (itr.hasNext()) {
					count = itr.next();
				}

				if (count == null) {
					count = new Long(0);
				}

				FinderCacheUtil.putResult(finderClassNameCacheEnabled,
					finderClassName, finderMethodName, finderParams,
					finderArgs, count);

				return count.intValue();
			}
			catch (Exception e) {
				throw processException(e);
			}
			finally {
				closeSession(session);
			}
		}
		else {
			return ((Long)result).intValue();
		}
	}

	public int countByN_H(long nodeId, boolean head) throws SystemException {
		boolean finderClassNameCacheEnabled = WikiPageModelImpl.CACHE_ENABLED;
		String finderClassName = WikiPage.class.getName();
		String finderMethodName = "countByN_H";
		String[] finderParams = new String[] {
				Long.class.getName(), Boolean.class.getName()
			};
		Object[] finderArgs = new Object[] {
				new Long(nodeId), Boolean.valueOf(head)
			};

		Object result = null;

		if (finderClassNameCacheEnabled) {
			result = FinderCacheUtil.getResult(finderClassName,
					finderMethodName, finderParams, finderArgs, this);
		}

		if (result == null) {
			Session session = null;

			try {
				session = openSession();

				StringBuilder query = new StringBuilder();

				query.append("SELECT COUNT(*) ");
				query.append(
					"FROM com.liferay.portlet.wiki.model.WikiPage WHERE ");

				query.append("nodeId = ?");

				query.append(" AND ");

				query.append("head = ?");

				query.append(" ");

				Query q = session.createQuery(query.toString());

				QueryPos qPos = QueryPos.getInstance(q);

				qPos.add(nodeId);

				qPos.add(head);

				Long count = null;

				Iterator<Long> itr = q.list().iterator();

				if (itr.hasNext()) {
					count = itr.next();
				}

				if (count == null) {
					count = new Long(0);
				}

				FinderCacheUtil.putResult(finderClassNameCacheEnabled,
					finderClassName, finderMethodName, finderParams,
					finderArgs, count);

				return count.intValue();
			}
			catch (Exception e) {
				throw processException(e);
			}
			finally {
				closeSession(session);
			}
		}
		else {
			return ((Long)result).intValue();
		}
	}

	public int countByN_P(long nodeId, String parentTitle)
		throws SystemException {
		boolean finderClassNameCacheEnabled = WikiPageModelImpl.CACHE_ENABLED;
		String finderClassName = WikiPage.class.getName();
		String finderMethodName = "countByN_P";
		String[] finderParams = new String[] {
				Long.class.getName(), String.class.getName()
			};
		Object[] finderArgs = new Object[] { new Long(nodeId), parentTitle };

		Object result = null;

		if (finderClassNameCacheEnabled) {
			result = FinderCacheUtil.getResult(finderClassName,
					finderMethodName, finderParams, finderArgs, this);
		}

		if (result == null) {
			Session session = null;

			try {
				session = openSession();

				StringBuilder query = new StringBuilder();

				query.append("SELECT COUNT(*) ");
				query.append(
					"FROM com.liferay.portlet.wiki.model.WikiPage WHERE ");

				query.append("nodeId = ?");

				query.append(" AND ");

				if (parentTitle == null) {
					query.append("parentTitle IS NULL");
				}
				else {
					query.append("parentTitle = ?");
				}

				query.append(" ");

				Query q = session.createQuery(query.toString());

				QueryPos qPos = QueryPos.getInstance(q);

				qPos.add(nodeId);

				if (parentTitle != null) {
					qPos.add(parentTitle);
				}

				Long count = null;

				Iterator<Long> itr = q.list().iterator();

				if (itr.hasNext()) {
					count = itr.next();
				}

				if (count == null) {
					count = new Long(0);
				}

				FinderCacheUtil.putResult(finderClassNameCacheEnabled,
					finderClassName, finderMethodName, finderParams,
					finderArgs, count);

				return count.intValue();
			}
			catch (Exception e) {
				throw processException(e);
			}
			finally {
				closeSession(session);
			}
		}
		else {
			return ((Long)result).intValue();
		}
	}

	public int countByN_R(long nodeId, String redirectTitle)
		throws SystemException {
		boolean finderClassNameCacheEnabled = WikiPageModelImpl.CACHE_ENABLED;
		String finderClassName = WikiPage.class.getName();
		String finderMethodName = "countByN_R";
		String[] finderParams = new String[] {
				Long.class.getName(), String.class.getName()
			};
		Object[] finderArgs = new Object[] { new Long(nodeId), redirectTitle };

		Object result = null;

		if (finderClassNameCacheEnabled) {
			result = FinderCacheUtil.getResult(finderClassName,
					finderMethodName, finderParams, finderArgs, this);
		}

		if (result == null) {
			Session session = null;

			try {
				session = openSession();

				StringBuilder query = new StringBuilder();

				query.append("SELECT COUNT(*) ");
				query.append(
					"FROM com.liferay.portlet.wiki.model.WikiPage WHERE ");

				query.append("nodeId = ?");

				query.append(" AND ");

				if (redirectTitle == null) {
					query.append("redirectTitle IS NULL");
				}
				else {
					query.append("redirectTitle = ?");
				}

				query.append(" ");

				Query q = session.createQuery(query.toString());

				QueryPos qPos = QueryPos.getInstance(q);

				qPos.add(nodeId);

				if (redirectTitle != null) {
					qPos.add(redirectTitle);
				}

				Long count = null;

				Iterator<Long> itr = q.list().iterator();

				if (itr.hasNext()) {
					count = itr.next();
				}

				if (count == null) {
					count = new Long(0);
				}

				FinderCacheUtil.putResult(finderClassNameCacheEnabled,
					finderClassName, finderMethodName, finderParams,
					finderArgs, count);

				return count.intValue();
			}
			catch (Exception e) {
				throw processException(e);
			}
			finally {
				closeSession(session);
			}
		}
		else {
			return ((Long)result).intValue();
		}
	}

	public int countByN_T_V(long nodeId, String title, double version)
		throws SystemException {
		boolean finderClassNameCacheEnabled = WikiPageModelImpl.CACHE_ENABLED;
		String finderClassName = WikiPage.class.getName();
		String finderMethodName = "countByN_T_V";
		String[] finderParams = new String[] {
				Long.class.getName(), String.class.getName(),
				Double.class.getName()
			};
		Object[] finderArgs = new Object[] {
				new Long(nodeId),
				
				title, new Double(version)
			};

		Object result = null;

		if (finderClassNameCacheEnabled) {
			result = FinderCacheUtil.getResult(finderClassName,
					finderMethodName, finderParams, finderArgs, this);
		}

		if (result == null) {
			Session session = null;

			try {
				session = openSession();

				StringBuilder query = new StringBuilder();

				query.append("SELECT COUNT(*) ");
				query.append(
					"FROM com.liferay.portlet.wiki.model.WikiPage WHERE ");

				query.append("nodeId = ?");

				query.append(" AND ");

				if (title == null) {
					query.append("title IS NULL");
				}
				else {
					query.append("title = ?");
				}

				query.append(" AND ");

				query.append("version = ?");

				query.append(" ");

				Query q = session.createQuery(query.toString());

				QueryPos qPos = QueryPos.getInstance(q);

				qPos.add(nodeId);

				if (title != null) {
					qPos.add(title);
				}

				qPos.add(version);

				Long count = null;

				Iterator<Long> itr = q.list().iterator();

				if (itr.hasNext()) {
					count = itr.next();
				}

				if (count == null) {
					count = new Long(0);
				}

				FinderCacheUtil.putResult(finderClassNameCacheEnabled,
					finderClassName, finderMethodName, finderParams,
					finderArgs, count);

				return count.intValue();
			}
			catch (Exception e) {
				throw processException(e);
			}
			finally {
				closeSession(session);
			}
		}
		else {
			return ((Long)result).intValue();
		}
	}

	public int countByN_T_H(long nodeId, String title, boolean head)
		throws SystemException {
		boolean finderClassNameCacheEnabled = WikiPageModelImpl.CACHE_ENABLED;
		String finderClassName = WikiPage.class.getName();
		String finderMethodName = "countByN_T_H";
		String[] finderParams = new String[] {
				Long.class.getName(), String.class.getName(),
				Boolean.class.getName()
			};
		Object[] finderArgs = new Object[] {
				new Long(nodeId),
				
				title, Boolean.valueOf(head)
			};

		Object result = null;

		if (finderClassNameCacheEnabled) {
			result = FinderCacheUtil.getResult(finderClassName,
					finderMethodName, finderParams, finderArgs, this);
		}

		if (result == null) {
			Session session = null;

			try {
				session = openSession();

				StringBuilder query = new StringBuilder();

				query.append("SELECT COUNT(*) ");
				query.append(
					"FROM com.liferay.portlet.wiki.model.WikiPage WHERE ");

				query.append("nodeId = ?");

				query.append(" AND ");

				if (title == null) {
					query.append("title IS NULL");
				}
				else {
					query.append("title = ?");
				}

				query.append(" AND ");

				query.append("head = ?");

				query.append(" ");

				Query q = session.createQuery(query.toString());

				QueryPos qPos = QueryPos.getInstance(q);

				qPos.add(nodeId);

				if (title != null) {
					qPos.add(title);
				}

				qPos.add(head);

				Long count = null;

				Iterator<Long> itr = q.list().iterator();

				if (itr.hasNext()) {
					count = itr.next();
				}

				if (count == null) {
					count = new Long(0);
				}

				FinderCacheUtil.putResult(finderClassNameCacheEnabled,
					finderClassName, finderMethodName, finderParams,
					finderArgs, count);

				return count.intValue();
			}
			catch (Exception e) {
				throw processException(e);
			}
			finally {
				closeSession(session);
			}
		}
		else {
			return ((Long)result).intValue();
		}
	}

	public int countByN_H_P(long nodeId, boolean head, String parentTitle)
		throws SystemException {
		boolean finderClassNameCacheEnabled = WikiPageModelImpl.CACHE_ENABLED;
		String finderClassName = WikiPage.class.getName();
		String finderMethodName = "countByN_H_P";
		String[] finderParams = new String[] {
				Long.class.getName(), Boolean.class.getName(),
				String.class.getName()
			};
		Object[] finderArgs = new Object[] {
				new Long(nodeId), Boolean.valueOf(head),
				
				parentTitle
			};

		Object result = null;

		if (finderClassNameCacheEnabled) {
			result = FinderCacheUtil.getResult(finderClassName,
					finderMethodName, finderParams, finderArgs, this);
		}

		if (result == null) {
			Session session = null;

			try {
				session = openSession();

				StringBuilder query = new StringBuilder();

				query.append("SELECT COUNT(*) ");
				query.append(
					"FROM com.liferay.portlet.wiki.model.WikiPage WHERE ");

				query.append("nodeId = ?");

				query.append(" AND ");

				query.append("head = ?");

				query.append(" AND ");

				if (parentTitle == null) {
					query.append("parentTitle IS NULL");
				}
				else {
					query.append("parentTitle = ?");
				}

				query.append(" ");

				Query q = session.createQuery(query.toString());

				QueryPos qPos = QueryPos.getInstance(q);

				qPos.add(nodeId);

				qPos.add(head);

				if (parentTitle != null) {
					qPos.add(parentTitle);
				}

				Long count = null;

				Iterator<Long> itr = q.list().iterator();

				if (itr.hasNext()) {
					count = itr.next();
				}

				if (count == null) {
					count = new Long(0);
				}

				FinderCacheUtil.putResult(finderClassNameCacheEnabled,
					finderClassName, finderMethodName, finderParams,
					finderArgs, count);

				return count.intValue();
			}
			catch (Exception e) {
				throw processException(e);
			}
			finally {
				closeSession(session);
			}
		}
		else {
			return ((Long)result).intValue();
		}
	}

	public int countAll() throws SystemException {
		boolean finderClassNameCacheEnabled = WikiPageModelImpl.CACHE_ENABLED;
		String finderClassName = WikiPage.class.getName();
		String finderMethodName = "countAll";
		String[] finderParams = new String[] {  };
		Object[] finderArgs = new Object[] {  };

		Object result = null;

		if (finderClassNameCacheEnabled) {
			result = FinderCacheUtil.getResult(finderClassName,
					finderMethodName, finderParams, finderArgs, this);
		}

		if (result == null) {
			Session session = null;

			try {
				session = openSession();

				Query q = session.createQuery(
						"SELECT COUNT(*) FROM com.liferay.portlet.wiki.model.WikiPage");

				Long count = null;

				Iterator<Long> itr = q.list().iterator();

				if (itr.hasNext()) {
					count = itr.next();
				}

				if (count == null) {
					count = new Long(0);
				}

				FinderCacheUtil.putResult(finderClassNameCacheEnabled,
					finderClassName, finderMethodName, finderParams,
					finderArgs, count);

				return count.intValue();
			}
			catch (Exception e) {
				throw processException(e);
			}
			finally {
				closeSession(session);
			}
		}
		else {
			return ((Long)result).intValue();
		}
	}

	public void registerListener(ModelListener listener) {
		List<ModelListener> listeners = ListUtil.fromArray(_listeners);

		listeners.add(listener);

		_listeners = listeners.toArray(new ModelListener[listeners.size()]);
	}

	public void unregisterListener(ModelListener listener) {
		List<ModelListener> listeners = ListUtil.fromArray(_listeners);

		listeners.remove(listener);

		_listeners = listeners.toArray(new ModelListener[listeners.size()]);
	}

	public void afterPropertiesSet() {
		String[] listenerClassNames = StringUtil.split(GetterUtil.getString(
					com.liferay.portal.util.PropsUtil.get(
						"value.object.listener.com.liferay.portlet.wiki.model.WikiPage")));

		if (listenerClassNames.length > 0) {
			try {
				List<ModelListener> listeners = new ArrayList<ModelListener>();

				for (String listenerClassName : listenerClassNames) {
					listeners.add((ModelListener)Class.forName(
							listenerClassName).newInstance());
				}

				_listeners = listeners.toArray(new ModelListener[listeners.size()]);
			}
			catch (Exception e) {
				_log.error(e);
			}
		}
	}

	private static Log _log = LogFactory.getLog(WikiPagePersistenceImpl.class);
	private ModelListener[] _listeners = new ModelListener[0];
}