import {createRouter, createWebHashHistory} from "vue-router";
import {unauthorized} from "@/utils";

const router = createRouter({
    history: createWebHashHistory(),
    routes: [
        {
            path: "/login",
            name: "Login",
            component: () => import('@/views/Login/index.vue'),
            meta: {requiresAuth: false},
        },
        {
            path: "/home",
            name: "Home",
            component: () => import('@/views/Home/index.vue'),
            meta: {requiresAuth: true},
        },
        {
            path: "/",
            redirect: "/login",
        }
    ],
});

// 路由守卫
router.beforeEach((to, from, next) => {
    const isUnauthorized = unauthorized();

    // 情况1: 用户未登录，且试图访问需要权限的页面
    // to.matched.some(...) 会检查当前路由及其所有父级路由是否有 requiresAuth
    if (to.meta.requiresAuth && isUnauthorized) {
        next('/login');
        return;
    }

    // 情况2: 用户已登录，但试图访问登录页 (Login)
    if (!isUnauthorized && to.name === 'Login') {
        next('/home');
        return;
    }

    // 其他情况直接放行
    next();
});

export default router;
