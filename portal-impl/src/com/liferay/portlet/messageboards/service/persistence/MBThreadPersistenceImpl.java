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

package com.liferay.portlet.messageboards.service.persistence;

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
import com.liferay.portal.model.ModelListener;
import com.liferay.portal.service.persistence.impl.BasePersistenceImpl;

import com.liferay.portlet.messageboards.NoSuchThreadException;
import com.liferay.portlet.messageboards.model.MBThread;
import com.liferay.portlet.messageboards.model.impl.MBThreadImpl;
import com.liferay.portlet.messageboards.model.impl.MBThreadModelImpl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * <a href="MBThreadPersistenceImpl.java.html"><b><i>View Source</i></b></a>
 *
 * @author Brian Wing Shun Chan
 *
 */
public class MBThreadPersistenceImpl extends BasePersistenceImpl
	implements MBThreadPersistence, InitializingBean {
	public MBThread create(long threadId) {
		MBThread mbThread = new MBThreadImpl();

		mbThread.setNew(true);
		mbThread.setPrimaryKey(threadId);

		return mbThread;
	}

	public MBThread remove(long threadId)
		throws NoSuchThreadException, SystemException {
		Session session = null;

		try {
			session = openSession();

			MBThread mbThread = (MBThread)session.get(MBThreadImpl.class,
					new Long(threadId));

			if (mbThread == null) {
				if (_log.isWarnEnabled()) {
					_log.warn("No MBThread exists with the primary key " +
						threadId);
				}

				throw new NoSuchThreadException(
					"No MBThread exists with the primary key " + threadId);
			}

			return remove(mbThread);
		}
		catch (NoSuchThreadException nsee) {
			throw nsee;
		}
		catch (Exception e) {
			throw processException(e);
		}
		finally {
			closeSession(session);
		}
	}

	public MBThread remove(MBThread mbThread) throws SystemException {
		if (_listeners.length > 0) {
			for (ModelListener listener : _listeners) {
				listener.onBeforeRemove(mbThread);
			}
		}

		mbThread = removeImpl(mbThread);

		if (_listeners.length > 0) {
			for (ModelListener listener : _listeners) {
				listener.onAfterRemove(mbThread);
			}
		}

		return mbThread;
	}

	protected MBThread removeImpl(MBThread mbThread) throws SystemException {
		Session session = null;

		try {
			session = openSession();

			session.delete(mbThread);

			session.flush();

			return mbThread;
		}
		catch (Exception e) {
			throw processException(e);
		}
		finally {
			closeSession(session);

			FinderCacheUtil.clearCache(MBThread.class.getName());
		}
	}

	/**
	 * @deprecated Use <code>update(MBThread mbThread, boolean merge)</code>.
	 */
	public MBThread update(MBThread mbThread) throws SystemException {
		if (_log.isWarnEnabled()) {
			_log.warn(
				"Using the deprecated update(MBThread mbThread) method. Use update(MBThread mbThread, boolean merge) instead.");
		}

		return update(mbThread, false);
	}

	/**
	 * Add, update, or merge, the entity. This method also calls the model
	 * listeners to trigger the proper events associated with adding, deleting,
	 * or updating an entity.
	 *
	 * @param        mbThread the entity to add, update, or merge
	 * @param        merge boolean value for whether to merge the entity. The
	 *                default value is false. Setting merge to true is more
	 *                expensive and should only be true when mbThread is
	 *                transient. See LEP-5473 for a detailed discussion of this
	 *                method.
	 * @return        true if the portlet can be displayed via Ajax
	 */
	public MBThread update(MBThread mbThread, boolean merge)
		throws SystemException {
		boolean isNew = mbThread.isNew();

		if (_listeners.length > 0) {
			for (ModelListener listener : _listeners) {
				if (isNew) {
					listener.onBeforeCreate(mbThread);
				}
				else {
					listener.onBeforeUpdate(mbThread);
				}
			}
		}

		mbThread = updateImpl(mbThread, merge);

		if (_listeners.length > 0) {
			for (ModelListener listener : _listeners) {
				if (isNew) {
					listener.onAfterCreate(mbThread);
				}
				else {
					listener.onAfterUpdate(mbThread);
				}
			}
		}

		return mbThread;
	}

	public MBThread updateImpl(
		com.liferay.portlet.messageboards.model.MBThread mbThread, boolean merge)
		throws SystemException {
		Session session = null;

		try {
			session = openSession();

			if (merge) {
				session.merge(mbThread);
			}
			else {
				if (mbThread.isNew()) {
					session.save(mbThread);
				}
			}

			session.flush();

			mbThread.setNew(false);

			return mbThread;
		}
		catch (Exception e) {
			throw processException(e);
		}
		finally {
			closeSession(session);

			FinderCacheUtil.clearCache(MBThread.class.getName());
		}
	}

	public MBThread findByPrimaryKey(long threadId)
		throws NoSuchThreadException, SystemException {
		MBThread mbThread = fetchByPrimaryKey(threadId);

		if (mbThread == null) {
			if (_log.isWarnEnabled()) {
				_log.warn("No MBThread exists with the primary key " +
					threadId);
			}

			throw new NoSuchThreadException(
				"No MBThread exists with the primary key " + threadId);
		}

		return mbThread;
	}

	public MBThread fetchByPrimaryKey(long threadId) throws SystemException {
		Session session = null;

		try {
			session = openSession();

			return (MBThread)session.get(MBThreadImpl.class, new Long(threadId));
		}
		catch (Exception e) {
			throw processException(e);
		}
		finally {
			closeSession(session);
		}
	}

	public List<MBThread> findByCategoryId(long categoryId)
		throws SystemException {
		boolean finderClassNameCacheEnabled = MBThreadModelImpl.CACHE_ENABLED;
		String finderClassName = MBThread.class.getName();
		String finderMethodName = "findByCategoryId";
		String[] finderParams = new String[] { Long.class.getName() };
		Object[] finderArgs = new Object[] { new Long(categoryId) };

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
					"FROM com.liferay.portlet.messageboards.model.MBThread WHERE ");

				query.append("categoryId = ?");

				query.append(" ");

				query.append("ORDER BY ");

				query.append("priority DESC, ");
				query.append("lastPostDate DESC");

				Query q = session.createQuery(query.toString());

				QueryPos qPos = QueryPos.getInstance(q);

				qPos.add(categoryId);

				List<MBThread> list = q.list();

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
			return (List<MBThread>)result;
		}
	}

	public List<MBThread> findByCategoryId(long categoryId, int start, int end)
		throws SystemException {
		return findByCategoryId(categoryId, start, end, null);
	}

	public List<MBThread> findByCategoryId(long categoryId, int start, int end,
		OrderByComparator obc) throws SystemException {
		boolean finderClassNameCacheEnabled = MBThreadModelImpl.CACHE_ENABLED;
		String finderClassName = MBThread.class.getName();
		String finderMethodName = "findByCategoryId";
		String[] finderParams = new String[] {
				Long.class.getName(),
				
				"java.lang.Integer", "java.lang.Integer",
				"com.liferay.portal.kernel.util.OrderByComparator"
			};
		Object[] finderArgs = new Object[] {
				new Long(categoryId),
				
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
					"FROM com.liferay.portlet.messageboards.model.MBThread WHERE ");

				query.append("categoryId = ?");

				query.append(" ");

				if (obc != null) {
					query.append("ORDER BY ");
					query.append(obc.getOrderBy());
				}

				else {
					query.append("ORDER BY ");

					query.append("priority DESC, ");
					query.append("lastPostDate DESC");
				}

				Query q = session.createQuery(query.toString());

				QueryPos qPos = QueryPos.getInstance(q);

				qPos.add(categoryId);

				List<MBThread> list = (List<MBThread>)QueryUtil.list(q,
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
			return (List<MBThread>)result;
		}
	}

	public MBThread findByCategoryId_First(long categoryId,
		OrderByComparator obc) throws NoSuchThreadException, SystemException {
		List<MBThread> list = findByCategoryId(categoryId, 0, 1, obc);

		if (list.size() == 0) {
			StringBuilder msg = new StringBuilder();

			msg.append("No MBThread exists with the key {");

			msg.append("categoryId=" + categoryId);

			msg.append(StringPool.CLOSE_CURLY_BRACE);

			throw new NoSuchThreadException(msg.toString());
		}
		else {
			return list.get(0);
		}
	}

	public MBThread findByCategoryId_Last(long categoryId, OrderByComparator obc)
		throws NoSuchThreadException, SystemException {
		int count = countByCategoryId(categoryId);

		List<MBThread> list = findByCategoryId(categoryId, count - 1, count, obc);

		if (list.size() == 0) {
			StringBuilder msg = new StringBuilder();

			msg.append("No MBThread exists with the key {");

			msg.append("categoryId=" + categoryId);

			msg.append(StringPool.CLOSE_CURLY_BRACE);

			throw new NoSuchThreadException(msg.toString());
		}
		else {
			return list.get(0);
		}
	}

	public MBThread[] findByCategoryId_PrevAndNext(long threadId,
		long categoryId, OrderByComparator obc)
		throws NoSuchThreadException, SystemException {
		MBThread mbThread = findByPrimaryKey(threadId);

		int count = countByCategoryId(categoryId);

		Session session = null;

		try {
			session = openSession();

			StringBuilder query = new StringBuilder();

			query.append(
				"FROM com.liferay.portlet.messageboards.model.MBThread WHERE ");

			query.append("categoryId = ?");

			query.append(" ");

			if (obc != null) {
				query.append("ORDER BY ");
				query.append(obc.getOrderBy());
			}

			else {
				query.append("ORDER BY ");

				query.append("priority DESC, ");
				query.append("lastPostDate DESC");
			}

			Query q = session.createQuery(query.toString());

			QueryPos qPos = QueryPos.getInstance(q);

			qPos.add(categoryId);

			Object[] objArray = QueryUtil.getPrevAndNext(q, count, obc, mbThread);

			MBThread[] array = new MBThreadImpl[3];

			array[0] = (MBThread)objArray[0];
			array[1] = (MBThread)objArray[1];
			array[2] = (MBThread)objArray[2];

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

	public List<MBThread> findAll() throws SystemException {
		return findAll(QueryUtil.ALL_POS, QueryUtil.ALL_POS, null);
	}

	public List<MBThread> findAll(int start, int end) throws SystemException {
		return findAll(start, end, null);
	}

	public List<MBThread> findAll(int start, int end, OrderByComparator obc)
		throws SystemException {
		boolean finderClassNameCacheEnabled = MBThreadModelImpl.CACHE_ENABLED;
		String finderClassName = MBThread.class.getName();
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

				query.append(
					"FROM com.liferay.portlet.messageboards.model.MBThread ");

				if (obc != null) {
					query.append("ORDER BY ");
					query.append(obc.getOrderBy());
				}

				else {
					query.append("ORDER BY ");

					query.append("priority DESC, ");
					query.append("lastPostDate DESC");
				}

				Query q = session.createQuery(query.toString());

				List<MBThread> list = (List<MBThread>)QueryUtil.list(q,
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
			return (List<MBThread>)result;
		}
	}

	public void removeByCategoryId(long categoryId) throws SystemException {
		for (MBThread mbThread : findByCategoryId(categoryId)) {
			remove(mbThread);
		}
	}

	public void removeAll() throws SystemException {
		for (MBThread mbThread : findAll()) {
			remove(mbThread);
		}
	}

	public int countByCategoryId(long categoryId) throws SystemException {
		boolean finderClassNameCacheEnabled = MBThreadModelImpl.CACHE_ENABLED;
		String finderClassName = MBThread.class.getName();
		String finderMethodName = "countByCategoryId";
		String[] finderParams = new String[] { Long.class.getName() };
		Object[] finderArgs = new Object[] { new Long(categoryId) };

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
					"FROM com.liferay.portlet.messageboards.model.MBThread WHERE ");

				query.append("categoryId = ?");

				query.append(" ");

				Query q = session.createQuery(query.toString());

				QueryPos qPos = QueryPos.getInstance(q);

				qPos.add(categoryId);

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
		boolean finderClassNameCacheEnabled = MBThreadModelImpl.CACHE_ENABLED;
		String finderClassName = MBThread.class.getName();
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
						"SELECT COUNT(*) FROM com.liferay.portlet.messageboards.model.MBThread");

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
						"value.object.listener.com.liferay.portlet.messageboards.model.MBThread")));

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

	private static Log _log = LogFactory.getLog(MBThreadPersistenceImpl.class);
	private ModelListener[] _listeners = new ModelListener[0];
}