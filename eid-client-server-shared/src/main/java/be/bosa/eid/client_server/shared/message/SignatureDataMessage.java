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

import be.bosa.eid.client_server.shared.annotation.HttpBody;
import be.bosa.eid.client_server.shared.annotation.HttpHeader;
import be.bosa.eid.client_server.shared.annotation.MessageDiscriminator;
import be.bosa.eid.client_server.shared.annotation.NotNull;
import be.bosa.eid.client_server.shared.annotation.PostConstruct;
import be.bosa.eid.client_server.shared.annotation.ProtocolStateAllowed;
import be.bosa.eid.client_server.shared.annotation.ResponsesAllowed;
import be.bosa.eid.client_server.shared.protocol.ProtocolState;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.List;

/**
 * Signature Data transfer object.
 *
 * @author Frank Cornelis
 */
@ResponsesAllowed(FinishedMessage.class)
@ProtocolStateAllowed(ProtocolState.SIGN)
public class SignatureDataMessage extends AbstractProtocolMessage {

	@HttpHeader(TYPE_HTTP_HEADER)
	@MessageDiscriminator
	public static final String TYPE = SignatureDataMessage.class.getSimpleName();

	@HttpHeader(HTTP_HEADER_PREFIX + "SignatureValueSize")
	@NotNull
	public Integer signatureValueSize;

	@HttpHeader(HTTP_HEADER_PREFIX + "SignCertFileSize")
	@NotNull
	public Integer signCertFileSize;

	@HttpHeader(HTTP_HEADER_PREFIX + "CaCertFileSize")
	@NotNull
	public Integer caCertFileSize;

	@HttpHeader(HTTP_HEADER_PREFIX + "RootCaCertFileSize")
	@NotNull
	public Integer rootCertFileSize;

	@HttpBody
	@NotNull
	public byte[] body;

	public SignatureDataMessage() {
	}

	public SignatureDataMessage(byte[] signatureValue, List<X509Certificate> signCertChain) throws CertificateEncodingException {
		this(signatureValue, signCertChain.get(0).getEncoded(), signCertChain.get(1).getEncoded(),
				signCertChain.get(2).getEncoded());
	}

	public SignatureDataMessage(byte[] signatureValue, byte[] signCertFile, byte[] citizenCaCertFile, byte[] rootCaCertFile) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			baos.write(signatureValue);
			baos.write(signCertFile);
			baos.write(citizenCaCertFile);
			baos.write(rootCaCertFile);
			this.body = baos.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		this.signatureValueSize = signatureValue.length;
		this.signCertFileSize = signCertFile.length;
		this.caCertFileSize = citizenCaCertFile.length;
		this.rootCertFileSize = rootCaCertFile.length;
	}

	private byte[] copy(byte[] source, int idx, int count) {
		byte[] result = new byte[count];
		System.arraycopy(source, idx, result, 0, count);
		return result;
	}

	@PostConstruct
	public void postConstruct() {
		int idx = 0;
		this.signatureValue = copy(this.body, idx, this.signatureValueSize);
		idx += this.signatureValueSize;

		byte[] signCertFile = copy(this.body, idx, this.signCertFileSize);
		idx += this.signCertFileSize;
		X509Certificate signCert = getCertificate(signCertFile);

		byte[] citizenCaCertFile = copy(this.body, idx, this.caCertFileSize);
		idx += this.caCertFileSize;
		X509Certificate citizenCaCert = getCertificate(citizenCaCertFile);

		byte[] rootCaCertFile = copy(this.body, idx, this.rootCertFileSize);
		idx += this.rootCertFileSize;
		X509Certificate rootCaCert = getCertificate(rootCaCertFile);

		this.certificateChain = new LinkedList<>();
		this.certificateChain.add(signCert);
		this.certificateChain.add(citizenCaCert);
		this.certificateChain.add(rootCaCert);
	}

	private X509Certificate getCertificate(byte[] certData) {
		CertificateFactory certificateFactory;
		try {
			certificateFactory = CertificateFactory.getInstance("X.509");
		} catch (CertificateException e) {
			throw new RuntimeException("cert factory error: " + e.getMessage(), e);
		}

		try {
			return (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(certData));
		} catch (CertificateException e) {
			/*
			 * Can happen in case of missing certificates. Missing certificates
			 * are represented by means of 1300 null bytes.
			 */
			return null;
		}
	}

	public byte[] signatureValue;

	public List<X509Certificate> certificateChain;
}
