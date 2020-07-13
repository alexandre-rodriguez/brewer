package com.algaworks.brewer.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

@Profile("prod")
@Configuration
@PropertySource(value = { "file:/${HOME}/.brewer-s3.properties" }, ignoreResourceNotFound = true)
public class S3Config {

	@Autowired
	private Environment env;
	
	@Bean 
	public AmazonS3 amazonS3() {
		AWSCredentials credenciais = new  BasicAWSCredentials(
			env.getProperty("AWS_ACCESS_KEY_ID"), env.getProperty("AWS_SECRET_ACCESS_KEY"));
		
		AmazonS3 amazonS3 = AmazonS3ClientBuilder.standard()
				.withCredentials(new AWSStaticCredentialsProvider(credenciais))
				.withRegion(Regions.SA_EAST_1)
				.build();

		return amazonS3;
	}

}
