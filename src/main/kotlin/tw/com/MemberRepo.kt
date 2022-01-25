package tw.com

import com.mongodb.WriteConcern
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Filters.eq
import io.quarkus.mongodb.FindOptions
import io.quarkus.mongodb.reactive.ReactiveMongoClient
import io.quarkus.mongodb.reactive.ReactiveMongoCollection;
import io.smallrye.mutiny.Multi
import org.bson.types.ObjectId
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.util.*
import java.util.regex.Pattern
import javax.annotation.PostConstruct
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

@ApplicationScoped
class MemberRepo {

    @Inject
    lateinit var reactiveMongoClient: ReactiveMongoClient

    @Inject
    lateinit var mongoClient: MongoClient

    private lateinit var reactiveCollUserModel: ReactiveMongoCollection<User>
    private lateinit var collUserModel: MongoCollection<User>

    @ConfigProperty(name = "mongodb.db.admin")
    private lateinit var adminDbName: String

    @ConfigProperty(name = "mongodb.coll.user")
    private lateinit var userColl: String

    @PostConstruct
    fun init() {
        reactiveCollUserModel = reactiveMongoClient
            .getDatabase(adminDbName)
            .getCollection(userColl ,User::class.java)

        collUserModel = mongoClient
            .getDatabase(adminDbName)
            .getCollection(userColl ,User::class.java)
            .withWriteConcern(WriteConcern.ACKNOWLEDGED)
    }

    fun getAllUser(): Multi<User> {
        return reactiveCollUserModel.find(FindOptions().sort(
            eq("registeredDate", -1)))
    }

    fun getUser(username: String): Multi<User> {
        val pattern = Pattern.compile("^$username.*\$", Pattern.CASE_INSENSITIVE)
        val filters = Filters.regex("username",pattern)
        return reactiveCollUserModel.find(filters)
    }

    fun getUserByUserId(userId: String?): Multi<User> {
        return reactiveCollUserModel.find(eq("userId",userId))
    }

    fun insertUser(user: User): User{
        try {
            val result = collUserModel.insertOne(user)
            if(result.wasAcknowledged()){
                val userId = result.insertedId!!.asObjectId().value
                user._id = userId
            }
        }catch (e: Exception){
            println(e.message)
            val username = user.username!!
            val pattern = Pattern.compile("^$username.*\$", Pattern.CASE_INSENSITIVE)
            val filters = Filters.regex("username",pattern)
            val list = collUserModel.find(filters).toList()
            val numList = mutableListOf<Int>()
            list.forEach {
                val result = convertStringToInt(it.username!!.substringAfter(username))
                if(result != null){
                    numList.add(result)
                }
            }
            var num = numList.maxOrNull()?:1
            val newUsername = "${username}${++num}"
            user.username = newUsername
            insertUser(user)
        }
        return user
    }

    fun convertStringToInt(substring: String): Int?{
        return try {
            val result = substring.toInt()
            if(result <= 0) null else result
        } catch (e: NumberFormatException) {
            null
        }
    }

    data class User(
            var _id: ObjectId? = null,
            var userId: String? = null,
            var username: String? = null,
            var registeredDate: Date? = null,
            var updateDate: Date? = null,
    )
}
