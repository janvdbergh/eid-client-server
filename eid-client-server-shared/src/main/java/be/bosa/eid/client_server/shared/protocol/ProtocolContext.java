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

package be.bosa.eid.client_server.shared.protocol;

/**
 * Interface for protocol context component.
 *
 * @author Frank Cornelis
 */
public interface ProtocolContext {

	/**
	 * Gives back the current protocol state.
	 */
	ProtocolState getProtocolState();

	/**
	 * Persists the protocol state in the protocol context.
	 */
	void setProtocolState(ProtocolState protocolState);

	/**
	 * Removes the protocol state from the protocol context.
	 */
	void removeProtocolState();
}
