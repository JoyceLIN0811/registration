package tw.com

import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

@ApplicationScoped
class MemberSvc {

    @Inject
    lateinit var memberRepo: MemberRepo

    fun createUser(username: String){
        memberRepo.insertUser(username)
    }
}