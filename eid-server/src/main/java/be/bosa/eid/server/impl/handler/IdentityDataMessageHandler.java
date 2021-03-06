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

package be.bosa.eid.server.impl.handler;

import be.bosa.eid.client_server.shared.message.ErrorCode;
import be.bosa.eid.client_server.shared.message.FinishedMessage;
import be.bosa.eid.client_server.shared.message.IdentityDataMessage;
import be.bosa.eid.server.Address;
import be.bosa.eid.server.Identity;
import be.bosa.eid.server.impl.RequestContext;
import be.bosa.eid.server.impl.ServiceLocator;
import be.bosa.eid.server.impl.tlv.TlvParser;
import be.bosa.eid.server.spi.AddressDTO;
import be.bosa.eid.server.spi.AuditService;
import be.bosa.eid.server.spi.CertificateSecurityException;
import be.bosa.eid.server.spi.ExpiredCertificateSecurityException;
import be.bosa.eid.server.spi.IdentityConsumerService;
import be.bosa.eid.server.spi.IdentityDTO;
import be.bosa.eid.server.spi.IdentityIntegrityService;
import be.bosa.eid.server.spi.RevokedCertificateSecurityException;
import be.bosa.eid.server.spi.TrustCertificateSecurityException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Message handler for the identity data message.
 *
 * @author Frank Cornelis
 */
@HandlesMessage(IdentityDataMessage.class)
public class IdentityDataMessageHandler implements MessageHandler<IdentityDataMessage> {

	private static final Log LOG = LogFactory.getLog(IdentityDataMessageHandler.class);

	/**
	 * Please use ROOT_CERT_SESSION_ATTRIBUTE instead.
	 */
	public static final String SKIP_NATIONAL_NUMBER_CHECK_INIT_PARAM_NAME = "SkipNationalNumberCheck";
	public static final String INCLUDE_DATA_FILES = "IncludeDataFiles";

	@InitParam(SKIP_NATIONAL_NUMBER_CHECK_INIT_PARAM_NAME)
	private boolean skipNationalNumberCheck;

	@InitParam(HelloMessageHandler.IDENTITY_INTEGRITY_SERVICE_INIT_PARAM_NAME)
	private ServiceLocator<IdentityIntegrityService> identityIntegrityServiceLocator;

	@InitParam(AuthenticationDataMessageHandler.AUDIT_SERVICE_INIT_PARAM_NAME)
	private ServiceLocator<AuditService> auditServiceLocator;

	@InitParam(HelloMessageHandler.IDENTITY_CONSUMER_INIT_PARAM_NAME)
	private ServiceLocator<IdentityConsumerService> identityConsumerLocator;

	public Object handleMessage(IdentityDataMessage message, Map<String, String> httpHeaders,
								HttpServletRequest request, HttpSession session) throws ServletException {
		LOG.debug("received identity data");

		LOG.debug("identity file size: " + message.idFile.length);
		// parse the identity files
		Identity identity = TlvParser.parse(message.idFile, Identity.class);

		RequestContext requestContext = new RequestContext(session);
		boolean includeAddress = requestContext.includeAddress();
		boolean includeCertificates = requestContext.includeCertificates();
		boolean includePhoto = requestContext.includePhoto();

		/*
		 * Check whether the answer is in-line with what we expected.
		 */
		Address address;
		if (message.addressFile != null) {
			LOG.debug("address file size: " + message.addressFile.length);
			if (!includeAddress) {
				throw new ServletException("Address included while not requested");
			}
			/*
			 * Address file can be null.
			 */
			address = TlvParser.parse(message.addressFile, Address.class);
		} else {
			if (includeAddress) {
				throw new ServletException("Address not included while requested");
			}
			address = null;
		}

		X509Certificate authnCert = includeCertificates ? getCertificate(message.authnCertFile) : null;
		X509Certificate signCert = includeCertificates ? getCertificate(message.signCertFile) : null;
		X509Certificate caCert = includeCertificates ? getCertificate(message.caCertFile) : null;
		X509Certificate rootCert = includeCertificates ? getCertificate(message.rootCertFile) : null;
		if (includeCertificates && (authnCert == null || signCert == null || caCert == null || rootCert == null)) {
			throw new ServletException("authn cert not included while requested");
		}

		IdentityIntegrityService identityIntegrityService = this.identityIntegrityServiceLocator.locateService();
		if (identityIntegrityService != null) {
			/*
			 * First check if all required identity data is available.
			 */
			if (message.identitySignatureFile == null) {
				throw new ServletException("identity signature data not included while request");
			}
			LOG.debug("identity signature file size: " + message.identitySignatureFile.length);
			if (includeAddress) {
				if (message.addressSignatureFile == null) {
					throw new ServletException("address signature data not included while requested");
				}
				LOG.debug("address signature file size: " + message.addressSignatureFile.length);
			}
			if (message.rrnCertFile == null) {
				throw new ServletException("national registry certificate not included while requested");
			}
			LOG.debug("RRN certificate file size: " + message.rrnCertFile.length);
			/*
			 * Run identity integrity checks.
			 */
			X509Certificate rrnCertificate = getCertificate(message.rrnCertFile);
			PublicKey rrnPublicKey = rrnCertificate.getPublicKey();
			verifySignature(rrnCertificate.getSigAlgName(), message.identitySignatureFile, rrnPublicKey, request, message.idFile);
			if (!this.skipNationalNumberCheck) {
				String authnUserId = (String) session.getAttribute(AuthenticationDataMessageHandler.AUTHENTICATED_USER_IDENTIFIER_SESSION_ATTRIBUTE);
				if (authnUserId != null) {
					if (!authnUserId.equals(identity.nationalNumber)) {
						throw new ServletException("national number mismatch");
					}
				}
			}
			if (includeAddress) {
				byte[] addressFile = trimRight(message.addressFile);
				verifySignature(rrnCertificate.getSigAlgName(), message.addressSignatureFile, rrnPublicKey, request,
						addressFile, message.identitySignatureFile);
			}
			LOG.debug("checking national registration certificate: " + rrnCertificate.getSubjectX500Principal());
			X509Certificate rootCertificate = getCertificate(message.rootCertFile);
			List<X509Certificate> rrnCertificateChain = new LinkedList<>();
			rrnCertificateChain.add(rrnCertificate);
			rrnCertificateChain.add(rootCertificate);
			try {
				identityIntegrityService.checkNationalRegistrationCertificate(rrnCertificateChain);
			} catch (ExpiredCertificateSecurityException e) {
				return new FinishedMessage(ErrorCode.CERTIFICATE_EXPIRED);
			} catch (RevokedCertificateSecurityException e) {
				return new FinishedMessage(ErrorCode.CERTIFICATE_REVOKED);
			} catch (TrustCertificateSecurityException e) {
				return new FinishedMessage(ErrorCode.CERTIFICATE_NOT_TRUSTED);
			} catch (CertificateSecurityException e) {
				return new FinishedMessage(ErrorCode.CERTIFICATE);
			} catch (Exception e) {
				if ("javax.ejb.EJBException".equals(e.getClass().getName())) {
					Exception exception;
					try {
						Method getCausedByExceptionMethod = e.getClass().getMethod("getCausedByException");
						exception = (Exception) getCausedByExceptionMethod.invoke(e, new Object[]{});
					} catch (Exception e2) {
						LOG.debug("error: " + e.getMessage(), e);
						throw new SecurityException("error retrieving the root cause: " + e2.getMessage());
					}
					if (exception instanceof ExpiredCertificateSecurityException) {
						return new FinishedMessage(ErrorCode.CERTIFICATE_EXPIRED);
					}
					if (exception instanceof RevokedCertificateSecurityException) {
						return new FinishedMessage(ErrorCode.CERTIFICATE_REVOKED);
					}
					if (exception instanceof TrustCertificateSecurityException) {
						return new FinishedMessage(ErrorCode.CERTIFICATE_NOT_TRUSTED);
					}
					if (exception instanceof CertificateSecurityException) {
						return new FinishedMessage(ErrorCode.CERTIFICATE);
					}
				}
				throw new SecurityException("error checking the NRN certificate: " + e.getMessage(), e);
			}
		}

		if (message.photoFile != null) {
			LOG.debug("photo file size: " + message.photoFile.length);
			if (!includePhoto) {
				throw new ServletException("photo include while not requested");
			}
			/*
			 * Photo integrity check.
			 */
			byte[] expectedPhotoDigest = identity.photoDigest;
			byte[] actualPhotoDigest = digestPhoto(Util.getDigestAlgo(expectedPhotoDigest.length), message.photoFile);
			if (!Arrays.equals(expectedPhotoDigest, actualPhotoDigest)) {
				throw new ServletException("photo digest incorrect");
			}
		} else {
			if (includePhoto) {
				throw new ServletException("photo not included while requested");
			}
		}

		/*
		 * Check the validity of the identity data as good as possible.
		 */
		GregorianCalendar cardValidityDateEndGregorianCalendar = identity.getCardValidityDateEnd();
		if (cardValidityDateEndGregorianCalendar != null) {
			Date now = new Date();
			Date cardValidityDateEndDate = cardValidityDateEndGregorianCalendar.getTime();
			if (now.after(cardValidityDateEndDate)) {
				throw new SecurityException("eID card has expired");
			}
		}

		// push the identity into the session
		Optional<IdentityConsumerService> identityService = Optional.ofNullable(identityConsumerLocator.locateService());
		String requestId = getRequestId(session);
		identityService.ifPresent(service -> service.setIdentity(requestId, Util.map(identity, IdentityDTO.class)));
		if (address != null) {
			identityService.ifPresent(service -> service.setAddress(requestId, Util.map(address, AddressDTO.class)));

		}
		if (message.photoFile != null) {
			identityService.ifPresent(service -> service.setPhoto(requestId, message.photoFile));
		}

		if (includeCertificates) {
			identityService.ifPresent(service -> service.setCertificates(requestId, authnCert, signCert, caCert, rootCert));

		}

		AuditService auditService = this.auditServiceLocator.locateService();
		if (auditService != null) {
			String userId = identity.nationalNumber;
			auditService.identified(userId);
		}

		return new FinishedMessage();
	}

	private byte[] trimRight(byte[] addressFile) {
		int idx;
		for (idx = 0; idx < addressFile.length; idx++) {
			if (0 == addressFile[idx]) {
				break;
			}
		}
		byte[] result = new byte[idx];
		System.arraycopy(addressFile, 0, result, 0, idx);
		return result;
	}

	private void verifySignature(String signAlgo, byte[] signatureData, PublicKey publicKey, HttpServletRequest request,
								 byte[]... data) throws ServletException {
		Signature signature;
		try {
			signature = Signature.getInstance(signAlgo);
		} catch (NoSuchAlgorithmException e) {
			throw new ServletException("algo error: " + e.getMessage(), e);
		}
		try {
			signature.initVerify(publicKey);
		} catch (InvalidKeyException e) {
			throw new ServletException("key error: " + e.getMessage(), e);
		}
		try {
			for (byte[] dataItem : data) {
				signature.update(dataItem);
			}
			boolean result = signature.verify(signatureData);
			if (!result) {
				AuditService auditService = this.auditServiceLocator.locateService();
				if (auditService != null) {
					String remoteAddress = request.getRemoteAddr();
					auditService.identityIntegrityError(remoteAddress);
				}
				throw new ServletException("signature incorrect");
			}
		} catch (SignatureException e) {
			AuditService auditService = this.auditServiceLocator.locateService();
			if (auditService != null) {
				String remoteAddress = request.getRemoteAddr();
				auditService.identityIntegrityError(remoteAddress);
			}
			throw new ServletException("signature error: " + e.getMessage(), e);
		}
	}

	/**
	 * Tries to parse the X509 certificate.
	 *
	 * @return the X509 certificate, or <code>null</code> in case of a DER decoding error.
	 */
	private X509Certificate getCertificate(byte[] certFile) {
		try {
			CertificateFactory certificateFactory = CertificateFactory.getInstance("X509");
			return (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(certFile));
		} catch (CertificateException e) {
			LOG.warn("certificate error: " + e.getMessage(), e);
			LOG.debug("certificate size: " + certFile.length);
			LOG.debug("certificate file content: " + Hex.encodeHexString(certFile));
			/*
			 * Missing eID authentication and eID non-repudiation certificates
			 * could become possible for future eID cards. A missing certificate
			 * is represented as a block of 1300 null bytes.
			 */
			if (1300 == certFile.length) {
				boolean missingCertificate = true;
				for (byte aCertFile : certFile) {
					if (0 != aCertFile) {
						missingCertificate = false;
					}
				}
				if (missingCertificate) {
					LOG.debug("the certificate data indicates a missing certificate");
				}
			}
			return null;
		}
	}

	private byte[] digestPhoto(String digestAlgoName, byte[] photoFile) {
		MessageDigest messageDigest;
		try {
			messageDigest = MessageDigest.getInstance(digestAlgoName);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("digest error: " + e.getMessage(), e);
		}
		return messageDigest.digest(photoFile);
	}

	public void init(ServletConfig config) {
		// empty
	}
}
