package com.hiddenswitch.proto3.net.util;

import ch.qos.logback.classic.Level;
import co.paralleluniverse.fibers.Suspendable;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.mongo.*;

import static io.vertx.ext.sync.Sync.awaitResult;

import java.util.List;

/**
 * There is only one Mongo.
 * <p>
 * Provide an easy way to access Mongo's methods in a sync pattern.
 */
public class Mongo {
	static Logger logger = LoggerFactory.getLogger(Mongo.class);
	static Mongo instance;
	MongoClient client;
	private LocalMongo localMongoServer;

	static {
		ch.qos.logback.classic.Logger mongoLogger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory
				.getLogger("org.mongodb.driver");
		mongoLogger.setLevel(Level.ERROR);
	}

	public static Mongo mongo() {
		if (instance == null) {
			instance = new Mongo();
		}

		return instance;
	}

	public Mongo startEmbedded() {
		if (localMongoServer == null) {
			logger.info("Starting Mongod embedded...");
			localMongoServer = new LocalMongo();
			try {
				localMongoServer.start();
			} catch (Exception e) {
				logger.error("Mongo failed to start.", e);
				return this;
			}
			logger.info("Started Mongod embedded.");
		} else {
			logger.info("Mongod already started.");
		}
		return this;
	}

	public Mongo connect(Vertx vertx, String connectionString) {
		if (client != null) {
			return this;
		}

		client = MongoClient.createShared(vertx, new JsonObject().put("connection_string", connectionString));
		return this;
	}

	public Mongo connect(Vertx vertx) {
		// Gets the connection string from the static field.
		return connect(vertx, "mongodb://localhost:27017");
	}

	public MongoClient client() {
		return client;
	}

	@Suspendable
	public String insert(String collection, JsonObject document) {
		return awaitResult(h -> client.insert(collection, document, h));
	}

	@Suspendable
	public String insertWithOptions(String collection, JsonObject document, WriteOption writeOption) {
		return awaitResult(h -> client.insertWithOptions(collection, document, writeOption, h));
	}

	@Suspendable
	public MongoClientUpdateResult updateCollection(String collection, JsonObject query, JsonObject update) {
		return awaitResult(h -> client.updateCollection(collection, query, update, h));
	}

	@Suspendable
	public MongoClientUpdateResult updateCollectionWithOptions(String collection, JsonObject query, JsonObject update, UpdateOptions options) {
		return awaitResult(h -> client.updateCollectionWithOptions(collection, query, update, options, h));
	}

	@Suspendable
	public List<JsonObject> find(String collection, JsonObject query) {
		return awaitResult(h -> client.find(collection, query, h));
	}

	@Suspendable
	public List<JsonObject> findWithOptions(String collection, JsonObject query, FindOptions options) {
		return awaitResult(h -> client.findWithOptions(collection, query, options, h));
	}

	@Suspendable
	public JsonObject findOne(String collection, JsonObject query, JsonObject fields) {
		return awaitResult(h -> client.findOne(collection, query, fields, h));
	}

	@Suspendable
	public Long count(String collection, JsonObject query) {
		return awaitResult(h -> client.count(collection, query, h));
	}

	@Suspendable
	public List<String> getCollections() {
		return awaitResult(h -> client.getCollections(h));
	}

	@Suspendable
	public Void createIndex(String collection, JsonObject key) {
		return awaitResult(h -> client.createIndex(collection, key, h));
	}

	@Suspendable
	public Void createIndexWithOptions(String collection, JsonObject key, IndexOptions options) {
		return awaitResult(h -> client.createIndexWithOptions(collection, key, options, h));
	}

	@Suspendable
	public Void dropIndex(String collection, String indexName) {
		return awaitResult(h -> client.dropIndex(collection, indexName, h));
	}
}