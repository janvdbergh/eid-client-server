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

package be.fedict.eid.applet.service.signer.cms;

import be.fedict.eid.applet.service.signer.SHA1WithRSAProxySignature;

import java.security.Provider;

/**
 * Security Provider for proxy signature implementation.
 *
 * @author Frank Cornelis
 */
public class CMSProvider extends Provider {

	public static final String NAME = "CMSProvider";

	public CMSProvider() {
		super(NAME, 1.0, "CMS Security Provider");
		put("Signature.SHA1withRSA", SHA1WithRSAProxySignature.class.getName());
	}
}
