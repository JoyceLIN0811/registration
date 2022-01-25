const app = new Vue({
    el: "#admin",
    data: {
        username:'',
        userList:[],
        currentPage:1,
        limit:10
    },
    created() {
        this.query()
    },
    watch: {
        'currentPage': function (){
            document.documentElement.scrollTop = 0
        }
    },
    computed:{
        pageLength: function (){
            let length = this.userList.length
            if(length == 0){
                return 1
            }
            return Math.ceil(length / this.limit)
        },
        users: function (){
            let userList = this.userList
            let end = this.currentPage * this.limit
            let start = end - this.limit
            return userList.slice(start,end)
        }
    },
    methods: {
        query: function(){
            let url = `/api/user`
            if(this.username.trim().length > 0){
                let encodeUsername = encodeURIComponent(this.username.trim());
                url = `/api/user?username=${encodeUsername}`
            }
            fetch(url).then(res => {
                if (res.ok) {
                    return res.json()
                }
            }).then(userList => {
                this.userList = userList
            })
        },
        changePage: function (page){
            this.currentPage = page
        },
        previousPage: function(){
            if(this.currentPage !== 1){
                this.currentPage = this.currentPage-1
            }
        },
        nextPage: function (pageLength){
            if(this.currentPage !== pageLength) {
                this.currentPage = this.currentPage + 1
            }
        },
    }
})