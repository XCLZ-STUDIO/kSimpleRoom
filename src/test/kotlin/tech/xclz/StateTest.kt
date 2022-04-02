package tech.xclz

import kotlin.test.Test
import kotlin.test.assertEquals
import tech.xclz.PlayerState.*
import tech.xclz.PlayerAction.*
import tech.xclz.core.DefaultState.*
import tech.xclz.core.buildStateMachine


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

    @Test
    fun testActionizableAndStatizable() {
        val machine = buildStateMachine {
            Start by {
                connect goto NotInRoom
            }

            NotInRoom by {
                create goto Manager
                join goto Member
                disconnect goto End
            }

            Manager by {
                leave goto NotInRoom
                disconnect goto ManagerIDLE
            }

            Member by {
                leave goto NotInRoom
                disconnect goto MemberIDLE
            }
        }
        var state = machine.initState
        assert(state.equals(Start))
        state = state.on(connect)
        assert(state.equals(NotInRoom))
        state = state.on(create)
        assert(state.equals(Manager))
        state = state.on(leave)
        assert(state.equals(NotInRoom))
        state = state.on(join)
        assert(state.equals(Member))
        state = state.on(disconnect)
        assert(state.equals(MemberIDLE))
    }


}