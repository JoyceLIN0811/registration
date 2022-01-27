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
        // 如果使用retry().indefinitely() 經測會卡在最後一筆request，因此改用 atMost()
        // 但壓測後，目前的寫法reactiveMongoClient 成功率較低，因此還是選用mongoClient 去實作
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
     *  新增使用者時，因 username 為 unique 唯一值，因此重複時將會進入 insertByUsernameSeq()
     *  以遞迴的方式找到新的 username 做寫入
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
     *  如果遇到使用者名稱重複，先將同名開頭的使用者抓出後，計算總數並撇除後方非數字結尾的使用者，
     *  剩餘數字結尾的使用者列中，尾數最大者加一為其新的使用者名稱
     *
     *  e.g
     *  欲註冊使用者名稱 username = "joyce"
     *  已存在資料庫的相同名稱開頭同使用者列 ["joyce","joyce123","joyceLin","joyce_","joyce0811"]
     *  取出 [123,811] => 使用者最後得到的名稱為 "joyce812"
     */
    fun insertByUsernameSeq(user: User){
        try {
            val insertUser = User()
            insertUser.username = user.username!!
            insertUser.userId = user.userId
            insertUser.registeredDate = user.registeredDate
            insertUser.updateDate = user.updateDate
            insertUser.username = getNewUserName(user.username!!)

            val result = collUserModel.insertOne(insertUser)
            if(result.wasAcknowledged()){
                val userId = result.insertedId!!.asObjectId().value
                user._id = userId
            }
        } catch (e: Exception){
            println(e.message)
            insertByUsernameSeq(user)
        }
    }

    fun getNewUserName(username: String): String{
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
