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

package be.bosa.eid.client_server.shared.message;

import be.bosa.eid.client_server.shared.annotation.HttpHeader;
import be.bosa.eid.client_server.shared.annotation.MessageDiscriminator;
import be.bosa.eid.client_server.shared.annotation.ResponsesAllowed;
import be.bosa.eid.client_server.shared.annotation.StartRequestMessage;
import be.bosa.eid.client_server.shared.protocol.ProtocolState;

/**
 * Hello Message transfer object.
 *
 * @author Frank Cornelis
 */
@ResponsesAllowed({IdentificationRequestMessage.class, CheckClientMessage.class, AuthenticationRequestMessage.class,
		AdministrationMessage.class, SignRequestMessage.class, FilesDigestRequestMessage.class,
		SignCertificatesRequestMessage.class, FinishedMessage.class})
@StartRequestMessage(ProtocolState.INIT)
public class HelloMessage extends AbstractProtocolMessage {

	@HttpHeader(TYPE_HTTP_HEADER)
	@MessageDiscriminator
	public static final String TYPE = HelloMessage.class.getSimpleName();

	@HttpHeader(HTTP_HEADER_PREFIX + "Language")
	public String language;

	@HttpHeader(HTTP_HEADER_PREFIX + "RequestId")
	public String requestId;

	public HelloMessage() {
		super();
	}

	public HelloMessage(String language, String requestId) {
		this.language = language;
		this.requestId = requestId;
	}
}
