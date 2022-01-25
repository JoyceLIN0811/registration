package tw.com

import io.smallrye.mutiny.Uni
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import tw.com.MemberCtrl.UserRegisterInfo
import tw.com.MemberRepo.User
import java.text.SimpleDateFormat
import java.util.*

@ApplicationScoped
class MemberSvc {

    @Inject
    lateinit var memberRepo: MemberRepo

    fun createUser(userRegisterInfo: UserRegisterInfo): User{
        val now = Date()
        val user = User(
            userId = userRegisterInfo.userId,
            username = userRegisterInfo.username,
            registeredDate = now,
            updateDate = now
        )
        return memberRepo.insertUser(user)
    }

    fun checkUserIdIsExist(userId: String): Uni<Boolean>{
        return memberRepo.getUserByUserId(userId)
            .collect().asList().map { it.isEmpty() }
    }

    fun getUsers(username: String?): Uni<List<UserView>> {
        val sdFormat = SimpleDateFormat("yyyy/MM/dd", Locale.TAIWAN)
        val multiUser = if(username != null){
            memberRepo.getUser(username)
        }else{
            memberRepo.getAllUser()
        }
        return multiUser.map{
                val userView = UserView()
                userView.userId = it.userId
                userView.username = it.username
                userView.registeredDate = sdFormat.format(it.registeredDate)
                userView
            }.collect().asList()
    }

    data class UserView(
        var userId: String? = null,
        var username: String? = null,
        var registeredDate: String? = null
    )
}