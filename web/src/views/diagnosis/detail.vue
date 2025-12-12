<template>
  <div class="container">
    <div class="record-container t-borderStyle">
        <el-card style="width: 100%;">
            <div slot="header" class="clearfix">
                <i class="el-icon-back" @click="goBack" style="cursor: pointer;margin-right: 5px;"></i>
                <span>病历详情</span>
            </div>
            <div>
                <div class="content">
                    <div style="line-height: 30px;"><span style="font-weight: bold;">患者姓名：</span>{{ record.patientName }}</div>
                    <div style="line-height: 30px;"><span style="font-weight: bold;">患者年龄：</span>{{ record.age }}</div>
                    <div style="line-height: 30px;"><span style="font-weight: bold;">患者联系方式：</span>{{ record.phone }}</div>
                    <div style="line-height: 30px;"><span style="font-weight: bold;">病情自述：</span>{{ record.description }}</div>
                    <div style="line-height: 30px;"><span style="font-weight: bold;">既往史：</span>{{ record.oldHistory || '-' }}</div>
                    <div style="line-height: 30px;"><span style="font-weight: bold;">过敏史：</span>{{ record.allergiesHistory || '-' }}</div>
                    <div style="line-height: 30px;"><span style="font-weight: bold;">生活习惯：</span>{{ record.habits || '-' }}</div>
                    <div style="line-height: 30px;"><span style="font-weight: bold;">所属科室：</span>{{ record.departmentName }}</div>
                    <div style="line-height: 30px;"><span style="font-weight: bold;">诊治医生：</span>{{ record.doctorName }}</div>
                </div>
                <!-- <div class="demo-image__preview">
                    <div>
                        <strong style="margin-bottom: 20px"
                        >病症图片:(点击查看大图)&nbsp;&nbsp;</strong
                        >
                    </div>
                    <el-image
                        style="width: 100px; height: 100px; margin-top: 20px"
                        :src="path + record.pic1"
                        :preview-src-list="[srcList[0]]"
                    >
                    </el-image>
                    <el-image
                        style="width: 100px; height: 100px; margin-left: 20px"
                        :src="path + record.pic2"
                        :preview-src-list="[srcList[1]]"
                    >
                    </el-image>
                    <el-image
                        v-if="record.pic3 != null && record.pic3 != ''"
                        style="width: 100px; height: 100px; margin-left: 20px"
                        :src="path + record.pic3"
                        :preview-src-list="[srcList[2]]"
                    >
                    </el-image>
                </div> -->
          </div>
    </el-card>
    </div>
    <div class="diagnosis-container t-borderStyle">
        <el-card style="width: 100%;">
            <div slot="header" class="clearfix">
                <span>诊断结果</span>
            </div>
            <el-form ref="diagnosisRef" :model="diagnostic" label-width="80px" :rules="rules">
                <el-form-item label="诊断结果" prop="result">
                    <el-input type="textarea" :disabled="record.status === 3" @blur="queryRecommendList" placeholder="输入您的诊断结果" v-model="diagnostic.result" ></el-input>
                </el-form-item>
                <el-form-item label="治疗方案" prop="prescription">
                    <el-input type="textarea" :disabled="record.status === 3" v-model="diagnostic.prescription" placeholder="输入您的治疗方案" ></el-input>
                </el-form-item>
                <el-form-item label="医嘱" prop="orders">
                    <el-input type="textarea" :disabled="record.status === 3" v-model="diagnostic.orders" placeholder="输入您的医嘱" ></el-input>
                </el-form-item>
                <div v-if="record.status!=3" style="display: flex;justify-content: center;gap: 20px;">
                    <el-button type="primary" @click="addDiagnosis">提交诊断</el-button>
                    <el-button type="primary" @click="reset">重置诊断</el-button>
                </div>
                
                <!-- 当状态为已诊断(status=2)时显示提示信息 -->
                <div v-if="record.status==2" style="text-align: center; margin-top: 15px;">
                    <el-alert
                        title="诊断已提交，等待患者确认"
                        type="info"
                        description="您已完成诊断。患者可在病历详情页查看诊断结果，并可选择结束诊断并提交评分。诊断结束状态(status=3)只能由患者触发。"
                        show-icon
                        :closable="false">
                    </el-alert>
                </div>
            </el-form>
        </el-card>
    </div>
    <div class="recommend-container t-borderStyle">
        <el-card style="width: 100%;">
            <div slot="header" class="clearfix">
                <span>药方推荐列表</span>
                <el-tooltip content="根据诊断结果自动推荐相似病例的治疗方案" placement="top">
                    <i class="el-icon-info" style="margin-left: 5px; color: #909399;"></i>
                </el-tooltip>
            </div>
            
            <div v-if="recommendLoading" class="loading-container">
                <el-spinner type="primary"></el-spinner>
                <p>正在分析诊断结果，查找相似病例...</p>
            </div>
            
            <div v-else-if="recommendList.length==0">
                <el-empty :image-size="200" description="暂无相似诊断推荐"></el-empty>
            </div>
            
            <div v-else>
                <div v-for="recommend in recommendList" :key="recommend.id" class="recommend-item">
                    <div class="recommend-header">
                        <span class="doctor-name">{{ recommend.docName }}</span>
                        <el-tag size="mini" type="success">相似度: {{ (recommend.similarityScore * 100).toFixed(0) }}%</el-tag>
                    </div>
                    
                    <div style="line-height: 30px;"><span style="font-weight: bold;">诊断结果：</span>{{ recommend.result }}</div>
                    
                    <div style="line-height: 30px; display: flex; align-items: center;">
                        <span style="font-weight: bold;">患者评分：</span>
                        <el-rate v-model="recommend.score" disabled show-score text-color="#ff9900" score-template="{value}"></el-rate>
                    </div>
                    
                    <div style="line-height: 30px;"><span style="font-weight: bold;">治疗方案/药方：</span>{{ recommend.prescription }}</div>
                    
                    <div class="recommend-actions">
                        <el-button @click="applyRecommendation(recommend)" type="text" size="small">
                            <i class="el-icon-check"></i> 应用此方案
                        </el-button>
                        <el-link @click="recommendDetail(recommend.recordId)" type="primary">查看详情</el-link>
                    </div>
                    
                    <el-divider></el-divider>
                </div>
            </div>
        </el-card>
    </div>
  </div>
</template>

<script>
import { getRecordInfo, updateRecord, getDiagnosisRecommendations } from '@/api/record';
import { isRequestCanceled, cancelAllRequests } from '@/utils/request';

export default {
  components: {

  },
  data () {
    return {
        record: {
          // 初始化空对象，避免访问属性时出错
          patientName: '',
          age: '',
          phone: '',
          description: '',
          oldHistory: '',
          allergiesHistory: '',
          habits: '',
          departmentName: '',
          doctorName: '',
          status: 0,
          diagnostic: null
        },
        recordId: null,
        diagnostic: {
            id: '',
            result: '',
            prescription: '',
            orders: ''
        },
        recommendList: [],
        recommendLoading: false,
        rules: {
            prescription: [
                { required: true, message: '请输入治疗方案', trigger: 'blur' }
            ],
            result: [
                { required: true, message: '请输入诊断结果', trigger: 'blur' }
            ],
            orders: [
                { required: false, message: '请输入医嘱', trigger: 'blur' }
            ]
        },
        // 标记组件是否已卸载
        isComponentUnmounted: false
    }
  },
  async mounted() {
    try {
      this.recordId = this.$route.query.recordId;
      if (!this.recordId) {
        this.$message.error('缺少病历ID参数');
        return;
      }
      
      // 先取消所有未完成的请求
      cancelAllRequests();
      
      const record = await getRecordInfo(this.recordId);
      
      // 检查组件是否已卸载
      if (this.isComponentUnmounted) return;
      
      if (record) {
        this.record = record;
        // 安全地访问诊断信息
    if (this.record.diagnostic) {
          this.diagnostic = {
            id: this.record.diagnostic.id || '',
            result: this.record.diagnostic.result || '',
            prescription: this.record.diagnostic.prescription || '',
            orders: this.record.diagnostic.orders || ''
          };
        }
        
        // 如果病历状态是未查看(0)，则更新为已查看(1)
        if (this.record.status === 0) {
          try {
            await updateRecord({
              recordId: this.recordId,
              status: 1
            });
            
            // 更新本地状态
            this.record.status = 1;
            
            // 通知诊断列表刷新数据
            this.$bus.$emit('refresh-diagnosis-list');
            
            console.log('病历状态已更新为已查看');
          } catch (error) {
            // 如果是请求取消错误，静默处理
            if (isRequestCanceled(error)) {
              return;
            }
            console.error('更新病历状态失败:', error);
          }
        }
      }
    } catch (error) {
      // 如果组件已卸载或是请求取消错误，静默处理
      if (this.isComponentUnmounted || isRequestCanceled(error)) {
        return;
      }
      console.error('获取病历详情失败:', error);
      this.$message.error('获取病历详情失败，请稍后再试');
    }
  },
  beforeDestroy() {
    // 标记组件已卸载
    this.isComponentUnmounted = true;
    
    // 取消所有未完成的请求
    cancelAllRequests();
  },
  methods: {
    async goBack() {
      try {
        // 如果病历状态是未查看，则更新为已查看
        if (this.record && this.record.status === 0 && this.recordId) {
        await updateRecord({
          recordId: this.recordId,
          status: 1
          });
          // 更新本地状态
          this.record.status = 1;
        }
        
        // 通知诊断列表刷新数据
        this.$bus.$emit('refresh-diagnosis-list');
        
        // 返回上一级
        this.$router.push('/home/diagnosisList');
      } catch (error) {
        // 如果是请求取消错误，静默处理
        if (isRequestCanceled(error)) {
          return;
        }
        console.error('更新病历状态失败:', error);
        // 即使更新失败也返回上一页
        this.$router.push('/home/diagnosisList');
      }
    },
    addDiagnosis() {
        // 提交诊断结果
        if (!this.$refs.diagnosisRef) return;
        
        this.$refs.diagnosisRef.validate(async valid => {
            if (valid) {
                try {
                    // 实现诊断提交逻辑
                    await updateRecord({
                        recordId: this.recordId,
                        status: 2,
                        diagnostic: {
                            result: this.diagnostic.result,
                            prescription: this.diagnostic.prescription,
                            orders: this.diagnostic.orders
                        }
                    });
                    
                    this.$message.success("诊断提交成功");
                    // 更新本地记录状态
                    this.record.status = 2;
                    
                    // 通知诊断列表刷新数据
                    this.$bus.$emit('refresh-diagnosis-list');
                    
                    // 提交成功后自动返回诊断列表页面
                    setTimeout(() => {
                        this.$router.push('/home/diagnosisList');
                    }, 1000);
                } catch (error) {
                    // 如果是请求取消错误，静默处理
                    if (isRequestCanceled(error)) {
                        return;
                    }
                    console.error('提交诊断失败:', error);
                    this.$message.error('提交诊断失败，请稍后再试');
                }
            }
        });
    },
    reset() {
        if (this.$refs.diagnosisRef) {
        this.$refs.diagnosisRef.resetFields();
        }
    },
    async queryRecommendList() {
        // 查询推荐列表逻辑
        if (!this.diagnostic.result) return;
        
        try {
            // 显示加载状态
            this.$set(this, 'recommendLoading', true);
            
            // 调用推荐API获取相似诊断
            const response = await getDiagnosisRecommendations(this.diagnostic.result);
            
            // 检查组件是否已卸载
            if (this.isComponentUnmounted) return;
            
            if (response && response.recommendations) {
                this.recommendList = response.recommendations.map(item => ({
                    id: item.diagnosticId,
                    docName: item.docName || '未知医生',
                    result: item.result || '未知诊断',
                    score: item.score || 0,
                    prescription: item.prescription || '无处方信息',
                    recordId: item.recordId,
                    similarityScore: item.similarityScore
                }));
                
                // 如果没有推荐结果，显示提示
                if (this.recommendList.length === 0) {
                    this.$message.info('未找到相似的诊断记录');
                }
            } else {
                this.recommendList = [];
                this.$message.warning('获取推荐列表失败');
            }
        } catch (error) {
            // 如果组件已卸载或是请求取消错误，静默处理
            if (this.isComponentUnmounted || isRequestCanceled(error)) {
                return;
            }
            console.error('获取推荐列表失败:', error);
            this.$message.error('获取推荐列表失败，请稍后再试');
            this.recommendList = [];
        } finally {
            // 隐藏加载状态
            if (!this.isComponentUnmounted) {
                this.$set(this, 'recommendLoading', false);
            }
        }
    },
    recommendDetail(recordId) {
        if (!recordId) return;
        
        // 如果点击的是当前记录，无需跳转
        if (recordId.toString() === this.recordId.toString()) {
            this.$message.info('已经在当前病历详情页');
            return;
        }
        
        // 先取消所有未完成的请求
        cancelAllRequests();
        
        // 跳转到推荐详情页
        this.$router.push({
            path: '/diagnosis/detail',
            query: { recordId }
        }).catch(err => {
            // 忽略重复导航错误
            if (err.name !== 'NavigationDuplicated') {
                console.error('路由跳转错误:', err);
                this.$message.error('页面跳转失败，请稍后再试');
            }
        });
    },
    
    /**
     * 应用推荐的诊断方案
     * @param {Object} recommend 推荐的诊断方案
     */
    applyRecommendation(recommend) {
        if (!recommend) return;
        
        // 显示确认对话框
        this.$confirm(`确定要应用"${recommend.docName}"医生的诊断方案吗？`, '应用推荐方案', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'info'
        }).then(() => {
            // 应用诊断方案
            this.diagnostic.result = recommend.result || this.diagnostic.result;
            this.diagnostic.prescription = recommend.prescription || this.diagnostic.prescription;
            
            // 如果有医嘱，也应用
            if (recommend.orders) {
                this.diagnostic.orders = recommend.orders;
            }
            
            this.$message.success('已应用推荐的诊断方案');
            
            // 提示用户可以修改
            this.$notify({
                title: '提示',
                message: '您可以根据实际情况修改应用的诊断方案',
                type: 'info',
                duration: 5000
            });
        }).catch(() => {
            // 用户取消，不做任何操作
        });
    }
  },
}

</script>
<style scoped lang='scss'>
.container {
    height: 100%;
    display: flex;
    gap: 20px;
}
.t-borderStyle {
    border-radius: 8px;
    box-shadow: 2px 2px 20px #f1dec2;
}
.record-container {
    width: 28%;
    height: 100%;
}
.recommend-container {
    width: 30%;
    height: 100%;
}
.diagnosis-container {
    flex: 1;
    height: 100%;
}

.loading-container {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 30px 0;
}

.recommend-item {
    padding: 10px 0;
}

.recommend-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 10px;
}

.doctor-name {
    font-weight: bold;
    font-size: 16px;
    color: var(--indigo-blue);
}

.recommend-actions {
    display: flex;
    justify-content: space-between;
    margin-top: 10px;
}


</style>