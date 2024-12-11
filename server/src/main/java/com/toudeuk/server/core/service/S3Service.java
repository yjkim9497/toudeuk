package com.toudeuk.server.core.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.util.IOUtils;
import com.toudeuk.server.core.exception.BaseException;
import com.toudeuk.server.core.exception.ErrorCode;
import com.toudeuk.server.domain.user.entity.User;
import com.toudeuk.server.domain.user.event.S3UploadEvent;
import com.toudeuk.server.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class S3Service {
	private final AmazonS3 s3;
	private final UserRepository userRepository;

	@Value("${cloud.aws.s3.bucket}")
	private String bucketName;

	@Value("${cloud.aws.s3.cloudFrontDomain}")
	private String cloudFrontDomain;

	// @Async("imageExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void upload(S3UploadEvent event) {

		String profileImage = event.getUser().getProfileImg();
		if (Objects.nonNull(profileImage)) {
			this.deleteImageFromS3(profileImage);
		}

		if (event.getFile().isEmpty() || Objects.isNull(event.getFile().getOriginalFilename())) {
			throw new BaseException(ErrorCode.EMPTY_FILE);
		}

		User user = event.getUser();

		CompletableFuture<String> future = CompletableFuture.completedFuture(this.uploadImage(event.getFile()));
		user.updateProfileImg(future.join());
		userRepository.save(user);
	}

	public void deleteImageFromS3(String imageAddress) {
		String key = getKeyFromImageAddress(imageAddress);
		try {
			s3.deleteObject(new DeleteObjectRequest(bucketName, key));
		} catch (Exception e) {
			throw new BaseException(ErrorCode.FAIL_TO_DELETE_FILE);
		}
	}

	private void validateImageFileExtention(String filename) {
		int lastDotIndex = filename.lastIndexOf(".");
		if (lastDotIndex == -1) {
			throw new BaseException(ErrorCode.NOT_SUPPORTED_EXTENTION);
		}

		String extention = filename.substring(lastDotIndex + 1).toLowerCase();
		List<String> allowedExtentionList = Arrays.asList("jpg", "jpeg", "png", "gif");

		if (!allowedExtentionList.contains(extention)) {
			throw new BaseException(ErrorCode.NOT_SUPPORTED_EXTENTION);
		}
	}

	private String uploadImage(MultipartFile image) {
		this.validateImageFileExtention(Objects.requireNonNull(image.getOriginalFilename()));
		try {
			return this.uploadToS3(image);
		} catch (IOException e) {
			throw new BaseException(ErrorCode.FAIL_TO_CREATE_FILE);
		}
	}

	private String uploadToS3(MultipartFile image) throws IOException {
		String originalFileName = image.getOriginalFilename();
		String extention = Objects.requireNonNull(originalFileName).substring(originalFileName.lastIndexOf("."));
		String s3FileName = UUID.randomUUID().toString().substring(0, 10) + originalFileName;
		InputStream is = image.getInputStream();

		byte[] bytes = IOUtils.toByteArray(is);

		ObjectMetadata metadata = new ObjectMetadata();

		metadata.setContentType("image/" + extention);
		metadata.setContentLength(bytes.length);

		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
		try {
			PutObjectRequest putRequest = new PutObjectRequest(bucketName, s3FileName, byteArrayInputStream, metadata);
			s3.putObject(putRequest);
		} catch (Exception e) {
			throw new BaseException(ErrorCode.FAIL_TO_CREATE_FILE);
		} finally {
			byteArrayInputStream.close();
			is.close();
		}

		String s3Url = s3.getUrl(bucketName, s3FileName).toString();
		log.info("s3Url : {}", s3Url);
		String cloudFrontUrl = s3Url.replace("https://s3.ap-northeast-2.amazonaws.com/toudeuk",
			cloudFrontDomain);
		log.info("cloudFrontUrl : {}", cloudFrontUrl);
		return cloudFrontUrl;
	}

	private String getKeyFromImageAddress(String imageAddress) {
		try {
			URL url = new URL(imageAddress);
			String decodingKey = URLDecoder.decode(url.getPath(), StandardCharsets.UTF_8);
			return decodingKey.substring(1);
		} catch (MalformedURLException e) {
			throw new BaseException(ErrorCode.FAIL_TO_DELETE_FILE);
		}
	}
}