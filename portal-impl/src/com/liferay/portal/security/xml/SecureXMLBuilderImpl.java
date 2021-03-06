/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.portal.security.xml;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.util.PropsValues;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLInputFactory;

import org.apache.xerces.parsers.SAXParser;

import org.xml.sax.XMLReader;

/**
 * @author Tomas Polesovsky
 */
public class SecureXMLBuilderImpl implements SecureXMLBuilder {

	@Override
	public DocumentBuilderFactory newDocumentBuilderFactory() {
		DocumentBuilderFactory documentBuilderFactory =
			DocumentBuilderFactory.newInstance();

		if (!PropsValues.XML_SECURITY_ENABLED) {
			return documentBuilderFactory;
		}

		try {
			documentBuilderFactory.setFeature(
				XMLConstants.FEATURE_SECURE_PROCESSING, true);
		}
		catch (Exception e) {
			_log.error(
				"Unable to initialize safe document builder factory to " +
					"protect from XML Bomb attacks",
				e);
		}

		try {
			documentBuilderFactory.setFeature(
				_FEATURES_DISALLOW_DOCTYPE_DECL, true);
		}
		catch (Exception e) {
			_log.error(
				"Unable to initialize safe document builder factory to " +
					"protect from XML Bomb attacks",
				e);
		}

		try {
			documentBuilderFactory.setFeature(
				_FEATURES_EXTERNAL_GENERAL_ENTITIES, false);

			documentBuilderFactory.setFeature(
				_FEATURES_EXTERNAL_PARAMETER_ENTITIES, false);
		}
		catch (Exception e) {
			_log.error(
				"Unable to initialize safe document builder factory to " +
					"protect from XXE attacks",
				e);
		}

		return documentBuilderFactory;
	}

	@Override
	public XMLInputFactory newXMLInputFactory() {
		XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();

		if (!PropsValues.XML_SECURITY_ENABLED) {
			return xmlInputFactory;
		}

		xmlInputFactory.setProperty(
			XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, Boolean.FALSE);
		xmlInputFactory.setProperty(
			XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
		xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);

		return xmlInputFactory;
	}

	@Override
	public XMLReader newXMLReader() {
		XMLReader xmlReader = new SAXParser();

		if (!PropsValues.XML_SECURITY_ENABLED) {
			return xmlReader;
		}

		try {
			xmlReader.setFeature(_FEATURES_DISALLOW_DOCTYPE_DECL, true);
		}
		catch (Exception e) {
			_log.error(
				"Unable to initialize safe SAX parser to protect from XML " +
					"Bomb attacks",
				e);
		}

		try {
			xmlReader.setFeature(_FEATURES_EXTERNAL_GENERAL_ENTITIES, false);
			xmlReader.setFeature(_FEATURES_EXTERNAL_PARAMETER_ENTITIES, false);
		}
		catch (Exception e) {
			_log.error(
				"Unable to initialize safe SAX parser to protect from XXE " +
					"attacks",
				e);
		}

		return xmlReader;
	}

	@Override
	public DocumentBuilderFactory unsafeDocumentBuilderFactory() {
		DocumentBuilderFactory documentBuilderFactory =
			newDocumentBuilderFactory();

		if (!PropsValues.XML_SECURITY_ENABLED) {
			return documentBuilderFactory;
		}

		try {
			documentBuilderFactory.setFeature(
				_FEATURES_DISALLOW_DOCTYPE_DECL, false);
		}
		catch (Exception e) {
			_log.error(
				"Unable to initialize unsafe document builder factory", e);
		}

		return documentBuilderFactory;
	}

	@Override
	public XMLInputFactory unsafeXMLInputFactory() {
		XMLInputFactory xmlInputFactory = newXMLInputFactory();

		if (!PropsValues.XML_SECURITY_ENABLED) {
			return xmlInputFactory;
		}

		xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.TRUE);

		return xmlInputFactory;
	}

	@Override
	public XMLReader unsafeXMLReader() {
		XMLReader xmlReader = newXMLReader();

		if (!PropsValues.XML_SECURITY_ENABLED) {
			return xmlReader;
		}

		try {
			xmlReader.setFeature(_FEATURES_DISALLOW_DOCTYPE_DECL, false);
		}
		catch (Exception e) {
			_log.error("Unable to initialize unsafe SAX parser", e);
		}

		return xmlReader;
	}

	private static final String _FEATURES_DISALLOW_DOCTYPE_DECL =
		"http://apache.org/xml/features/disallow-doctype-decl";

	private static final String _FEATURES_EXTERNAL_GENERAL_ENTITIES =
		"http://xml.org/sax/features/external-general-entities";

	private static final String _FEATURES_EXTERNAL_PARAMETER_ENTITIES =
		"http://xml.org/sax/features/external-parameter-entities";

	private static final Log _log = LogFactoryUtil.getLog(
		SecureXMLBuilderImpl.class);

}