/*
 * eID Client - Server Project.
 * Copyright (C) 2018 - 2018 BOSA.
 *
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License version 3.0 as published by
 * the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, see https://www.gnu.org/licenses/.
 */

package be.bosa.eid.server.impl.tlv;

import be.bosa.eid.server.SpecialOrganisation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.UnsupportedEncodingException;

/**
 * Data convertor for special organisation eID identity field.
 *
 * @author Frank Cornelis
 */
public class SpecialOrganisationConvertor implements DataConvertor<SpecialOrganisation> {

	private static final Log LOG = LogFactory.getLog(SpecialOrganisationConvertor.class);

	public SpecialOrganisation convert(byte[] value) throws DataConvertorException {
		if (value == null) {
			return SpecialOrganisation.UNSPECIFIED;
		}

		try {
			String key = new String(value, "UTF-8");
			LOG.debug("key: \"" + key + "\"");
			return SpecialOrganisation.toSpecialOrganisation(key);
		} catch (UnsupportedEncodingException e) {
			throw new DataConvertorException("string error: " + e.getMessage());
		}
	}
}
