/*
 * Copyright (c) SiteWhere, LLC. All rights reserved. http://www.sitewhere.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.sitewhere.device.persistence.mongodb;

import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.bson.types.Binary;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitewhere.mongodb.MongoConverter;
import com.sitewhere.mongodb.common.MongoMetadataProvider;
import com.sitewhere.mongodb.common.MongoSiteWhereEntity;
import com.sitewhere.rest.model.asset.DefaultAssetReferenceEncoder;
import com.sitewhere.rest.model.device.DeviceSpecification;
import com.sitewhere.rest.model.device.element.DeviceElementSchema;
import com.sitewhere.spi.device.DeviceContainerPolicy;
import com.sitewhere.spi.device.IDeviceSpecification;

/**
 * Used to load or save device specification data to MongoDB.
 * 
 * @author dadams
 */
public class MongoDeviceSpecification implements MongoConverter<IDeviceSpecification> {

    /** Static logger instance */
    private static Logger LOGGER = LogManager.getLogger();

    /** Property for id */
    public static final String PROP_ID = "id";

    /** Property for unique token */
    public static final String PROP_TOKEN = "tk";

    /** Property for specification name */
    public static final String PROP_NAME = "nm";

    /** Property for asset module id */
    public static final String PROP_ASSET_REFERENCE = "ar";

    /** Property for container policy */
    public static final String PROP_CONTAINER_POLICY = "cp";

    /** Property for device element schema */
    public static final String PROP_DEVICE_ELEMENT_SCHEMA = "es";

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.mongodb.MongoConverter#convert(java.lang.Object)
     */
    @Override
    public Document convert(IDeviceSpecification source) {
	return MongoDeviceSpecification.toDocument(source);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.mongodb.MongoConverter#convert(com.mongodb.DBObject)
     */
    @Override
    public IDeviceSpecification convert(Document source) {
	return MongoDeviceSpecification.fromDocument(source);
    }

    /**
     * Copy information from SPI into Mongo DBObject.
     * 
     * @param source
     * @param target
     */
    public static void toDocument(IDeviceSpecification source, Document target) {
	target.append(PROP_ID, source.getId());
	target.append(PROP_TOKEN, source.getToken());
	target.append(PROP_NAME, source.getName());
	target.append(PROP_ASSET_REFERENCE, new DefaultAssetReferenceEncoder().encode(source.getAssetReference()));
	target.append(PROP_CONTAINER_POLICY, source.getContainerPolicy().name());
	MongoSiteWhereEntity.toDocument(source, target);
	MongoMetadataProvider.toDocument(source, target);

	// Marshal device element schema as JSON.
	if (source.getDeviceElementSchema() != null) {
	    ObjectMapper mapper = new ObjectMapper();
	    try {
		byte[] schemaJson = mapper.writeValueAsBytes(source.getDeviceElementSchema());
		target.append(PROP_DEVICE_ELEMENT_SCHEMA, new Binary(schemaJson));
	    } catch (JsonProcessingException e) {
		LOGGER.error("Unable to marshal device element schema for MongoDB persistence.", e);
	    }
	}
    }

    /**
     * Copy information from Mongo DBObject to model object.
     * 
     * @param source
     * @param target
     */
    public static void fromDocument(Document source, DeviceSpecification target) {
	UUID id = (UUID) source.get(PROP_ID);
	String token = (String) source.get(PROP_TOKEN);
	String name = (String) source.get(PROP_NAME);
	String assetReference = (String) source.get(PROP_ASSET_REFERENCE);
	String containerPolicy = (String) source.get(PROP_CONTAINER_POLICY);
	Binary schemaBytes = (Binary) source.get(PROP_DEVICE_ELEMENT_SCHEMA);

	target.setId(id);
	target.setToken(token);
	target.setName(name);
	target.setAssetReference(new DefaultAssetReferenceEncoder().decode(assetReference));

	if (containerPolicy != null) {
	    target.setContainerPolicy(DeviceContainerPolicy.valueOf(containerPolicy));
	}

	// Unmarshal device element schema.
	if (schemaBytes != null) {
	    ObjectMapper mapper = new ObjectMapper();
	    try {
		DeviceElementSchema schema = mapper.readValue(schemaBytes.getData(), DeviceElementSchema.class);
		target.setDeviceElementSchema(schema);
	    } catch (Throwable e) {
		LOGGER.error("Unable to unmarshal device element schema from MongoDB persistence.", e);
	    }
	}

	MongoSiteWhereEntity.fromDocument(source, target);
	MongoMetadataProvider.fromDocument(source, target);
    }

    /**
     * Convert SPI object to Mongo {@link Document}.
     * 
     * @param source
     * @return
     */
    public static Document toDocument(IDeviceSpecification source) {
	Document result = new Document();
	MongoDeviceSpecification.toDocument(source, result);
	return result;
    }

    /**
     * Convert a {@link Document} into the SPI equivalent.
     * 
     * @param source
     * @return
     */
    public static DeviceSpecification fromDocument(Document source) {
	DeviceSpecification result = new DeviceSpecification();
	MongoDeviceSpecification.fromDocument(source, result);
	return result;
    }
}