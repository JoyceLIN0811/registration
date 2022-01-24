package tw.com

import io.smallrye.mutiny.Uni
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import tw.com.MemberCtrl.UserRegisterInfo
import tw.com.MemberRepo.User
import java.util.*

@ApplicationScoped
class MemberSvc {

    @Inject
    lateinit var memberRepo: MemberRepo

    fun createUser(userRegisterInfo: UserRegisterInfo): User{
        val user = User(
            userId = userRegisterInfo.userId,
            username = userRegisterInfo.username,
            createDate = Date()
        )
        return memberRepo.insertUser(user)
    }

    fun checkUserIdIsExist(userId: String): Uni<Boolean>{
        return memberRepo.getUserByUserId(userId)
            .collect().asList().map { it.isEmpty() }
    }

    fun getUsers(): Uni<List<User>> {
        return memberRepo.getUserList()
    }

    fun getUserByUserName(username: String?): Uni<User?> {
        return memberRepo.getUser(username)
    }
}