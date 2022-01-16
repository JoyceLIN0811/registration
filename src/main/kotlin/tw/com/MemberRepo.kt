package tw.com

import com.mongodb.WriteConcern
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters
import io.quarkus.mongodb.reactive.ReactiveMongoClient
import io.quarkus.mongodb.reactive.ReactiveMongoCollection;
import org.bson.types.ObjectId
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.util.*
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

    fun insertUser(username: String): User?{
        val user = User(
            username = username,
            createDate = Date()
        )
        try {
            collUserModel.insertOne(user)
        }catch (e: Exception){
            println(e.message)
            val filters = Filters.regex("username","/^$username/")
            val list = collUserModel.find(filters).toList()
            val numList = mutableListOf<Int>()
            list.forEach {
                val result = convertStringToInt(it.username!!.substringAfter("a"))
                if(result != null){
                    numList.add(result)
                }
            }
            var num = numList.maxOrNull()?:1
            user.username = "${username}${++num}"
            collUserModel.insertOne(user)
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
            var username: String? = null,
            var createDate: Date? = null,
    )
}
