package tw.com

import com.mongodb.WriteConcern
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.result.InsertOneResult
import io.quarkus.mongodb.FindOptions
import io.quarkus.mongodb.reactive.ReactiveMongoClient
import io.quarkus.mongodb.reactive.ReactiveMongoCollection;
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
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

    fun getUserByUserName(username: String): Multi<User> {
        val pattern = Pattern.compile("^$username.*\$", Pattern.CASE_INSENSITIVE)
        val filters = Filters.regex("username",pattern)
        return reactiveCollUserModel.find(filters)
    }

    fun getUserByUserId(userId: String?): Multi<User> {
        return reactiveCollUserModel.find(eq("userId",userId))
    }

    fun insertUserViaReactive(user: User): Uni<User> {
        val insertResult = reactiveCollUserModel.insertOne(user)
        // ????????????retry().indefinitely() ???????????????????????????request??????????????? atMost()
        // ??????????????????????????????reactiveMongoClient ????????????????????????????????????mongoClient ?????????
        return insertResult
            .onFailure().recoverWithUni{
                    _ -> insertByUsernameSeqViaReactive(user)
                .onFailure().retry().atMost(100) }
            .onItem().invoke {
                    result -> user._id = ObjectId(result.insertedId.asObjectId().value.toString()) }
            .map { user }
    }

    fun insertByUsernameSeqViaReactive(user: User): Uni<InsertOneResult>{
        user.username = getNewUserName(user.username!!)
        return reactiveCollUserModel.insertOne(user)
    }

    /**
     *  ???????????????????????? username ??? unique ??????????????????????????????????????? insertByUsernameSeq()
     *  ?????????????????????????????? username ?????????
     */
    fun insertUser(user: User): User{
        try {
            val result = collUserModel.insertOne(user)
            if(result.wasAcknowledged()){
                val userId = result.insertedId!!.asObjectId().value
                user._id = userId
            }
        }catch (e: Exception){
            println(e.message)
            insertByUsernameSeq(user)
        }
        return user
    }

    /**
     *  ???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     *  ???????????????????????????????????????????????????????????????????????????????????????
     *
     *  e.g
     *  ???????????????????????? username = "joyce"
     *  ?????????????????????????????????????????????????????? ["joyce","joyce123","joyceLin","joyce_","joyce0811"]
     *  ?????? [123,811] => ????????????????????????????????? "joyce812"
     */
    fun insertByUsernameSeq(user: User){
        val num = getLastSeq(user.username!!)
        try {
            val insertUser = User()
            insertUser.userId = user.userId
            insertUser.registeredDate = user.registeredDate
            insertUser.updateDate = user.updateDate
            insertUser.username = "${user.username!!}${num}"

            val result = collUserModel.insertOne(insertUser)
            if(result.wasAcknowledged()){
                val userId = result.insertedId!!.asObjectId().value
                user._id = userId
            }
        } catch (e: Exception){
//            println(e.message)
            increaseUsernameSeq(user,num)
//            insertByUsernameSeq(user)
        }
    }

    fun increaseUsernameSeq(user: User, seq: Int){
        val latestSeq = seq + 1
        try {
            val insertUser = User(
                username = "${user.username!!}${latestSeq}",
                userId = user.userId!!,
                registeredDate = user.registeredDate!!,
                updateDate = user.updateDate!!
            )
            val result = collUserModel.insertOne(insertUser)
            if(result.wasAcknowledged()){
                val userId = result.insertedId!!.asObjectId().value
                user._id = userId
            }
        } catch (e: Exception){
//            println(e.message)
            increaseUsernameSeq(user,latestSeq)
        }
    }

    fun getLastSeq(username: String): Int{
        val pattern = Pattern.compile("^$username.*\$", Pattern.CASE_INSENSITIVE)
        val filters = Filters.regex("username",pattern)
        val before = System.currentTimeMillis()
        val list = collUserModel.find(filters).toList()
        val after = System.currentTimeMillis()
        println("********************************")
        println("currentTimeMillis=${after-before}")
        val numList = mutableListOf<Int>()
        list.forEach {
            val result = convertStringToInt(it.username!!.substringAfter(username))
            if(result != null){
                numList.add(result)
            }
        }
        var num = numList.maxOrNull()?:1
        return ++num
    }

    fun getNewUserName(username: String): String{
        val pattern = Pattern.compile("^$username.*\$", Pattern.CASE_INSENSITIVE)
        val filters = Filters.regex("username",pattern)
        val before = System.currentTimeMillis()
        val list = collUserModel.find(filters).toList()
        val after = System.currentTimeMillis()
        println("********************************")
        println("currentTimeMillis=${after-before}")
        val numList = mutableListOf<Int>()
        list.forEach {
            val result = convertStringToInt(it.username!!.substringAfter(username))
            if(result != null){
                numList.add(result)
            }
        }
        var num = numList.maxOrNull()?:1
        return "${username}${++num}"
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
