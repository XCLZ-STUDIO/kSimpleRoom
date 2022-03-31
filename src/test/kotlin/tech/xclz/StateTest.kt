package tech.xclz

import kotlin.test.Test
import kotlin.test.assertEquals

class StateTest {
    @Test
    fun test() {
        val machine = buildStateMachine {
            "*" by {
                "建立连接" goto "未加入房间"
            }

            "未加入房间" by {
                "创建房间" goto "房主"
                "加入房间" goto "成员"
                "断开连接" goto "*"
            }

            "未加入房间" by {
                "房主" goto "创建房间"
                "成员" goto "加入房间"
                "*" goto "断开连接"
            }

            "房主" by {
                "退出房间" goto "未加入房间"
                "断开连接" goto "房主待恢复连接"
            }

            "成员" by {
                "退出房间" goto "未加入房间"
                "断开连接" goto "成员待恢复连接"
            }
        }
        var state = machine.initState
        assertEquals("*", state.name)
        state = state.on("建立连接")
        assertEquals("未加入房间", state.name)
        state = state.on("创建房间")
        assertEquals("房主", state.name)
        state = state.on("退出房间")
        assertEquals("未加入房间", state.name)
        state = state.on("加入房间")
        assertEquals("成员", state.name)
        state = state.on("断开连接")
        assertEquals("成员待恢复连接", state.name)
    }
}