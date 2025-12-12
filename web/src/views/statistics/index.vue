<template>
  <div class="statistics-container">
    <el-card class="statistics-card">
      <div class="card-header">
        <div class="header-left">
      <h2>统计分析</h2>
          <div class="header-decoration">
            <span class="decoration-line"></span>
            <span class="decoration-dot"></span>
          </div>
        </div>
        <div class="header-right">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            size="small"
            @change="handleDateChange"
          ></el-date-picker>
          <el-button size="small" type="primary" icon="el-icon-refresh" @click="refreshData">刷新</el-button>
        </div>
      </div>
      
      <!-- 数据概览卡片 -->
      <div class="data-overview">
        <el-row :gutter="20">
          <el-col :span="6">
            <div class="stat-card">
              <div class="stat-icon doctor-icon">
                <i class="el-icon-s-custom"></i>
              </div>
              <div class="stat-content">
                <div class="stat-title">医生总数</div>
                <div class="stat-value" v-if="!loading">{{ displayDoctorCount }}</div>
                <div class="stat-value" v-else><el-skeleton-item variant="text" style="width: 30%"></el-skeleton-item></div>
              </div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-card">
              <div class="stat-icon record-icon">
                <i class="el-icon-document"></i>
              </div>
              <div class="stat-content">
                <div class="stat-title">病例总数</div>
                <div class="stat-value" v-if="!loading">{{ displayRecordCount }}</div>
                <div class="stat-value" v-else><el-skeleton-item variant="text" style="width: 30%"></el-skeleton-item></div>
              </div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-card">
              <div class="stat-icon completed-icon">
                <i class="el-icon-finished"></i>
              </div>
              <div class="stat-content">
                <div class="stat-title">已完成诊断</div>
                <div class="stat-value" v-if="!loading">{{ completedRecords }}</div>
                <div class="stat-value" v-else><el-skeleton-item variant="text" style="width: 30%"></el-skeleton-item></div>
              </div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-card">
              <div class="stat-icon rating-icon">
                <i class="el-icon-star-on"></i>
              </div>
              <div class="stat-content">
                <div class="stat-title">平均评分</div>
                <div class="stat-value" v-if="!loading">{{ isNaN(averageScore) ? '0.0' : averageScore.toFixed(1) }}</div>
                <div class="stat-value" v-else><el-skeleton-item variant="text" style="width: 30%"></el-skeleton-item></div>
              </div>
            </div>
          </el-col>
        </el-row>
    </div>
      
      <!-- 图表区域 -->
    <div class="chart-container" style="margin-bottom: 0;">
        <el-row :gutter="20">
          <el-col :xs="24" :sm="24" :md="24" :lg="12" :xl="12">
            <el-card class="chart-card" shadow="hover" :body-style="{ padding: '10px' }">
              <div class="chart-header">
                <h3>医生评分排名</h3>
                <el-tooltip content="展示各医生的平均评分排名" placement="top">
                  <i class="el-icon-info"></i>
                </el-tooltip>
              </div>
              <div class="chart-body" ref="scoreChart"></div>
            </el-card>
          </el-col>
          <el-col :xs="24" :sm="24" :md="24" :lg="12" :xl="12">
            <el-card class="chart-card" shadow="hover" :body-style="{ padding: '10px' }">
              <div class="chart-header">
                <h3>诊断状态分布</h3>
                <el-tooltip content="展示不同诊断状态的病例数量分布" placement="top">
                  <i class="el-icon-info"></i>
                </el-tooltip>
              </div>
              <div class="chart-body" ref="statusChart"></div>
            </el-card>
          </el-col>
        </el-row>
    </div>
  </el-card>
  </div>
</template>

<script>
import * as echarts from 'echarts';
import { getRecordList } from '@/api/record';
import { getDoctorList } from '@/api/doctor';
import { cancelAllRequests, isRequestCanceled } from '@/utils/request';

export default {
  data() {
    return {
        scoreChart: null,
      statusChart: null,
        recordList: [],
        tableParams: {
            page: 1,
            pageSize: 10000,
        },
        doctorList: [],
      averageScores: {},
      dateRange: null,
      loading: true,
      charts: [],
      staticData: {
        doctors: 10,
        records: 20,
        completed: 6,
        avgScore: 4.0
      },
      hasError: false,
      chartsInitialized: false
    };
  },
  computed: {
    userInfo() {    
        return this.$store.state.user.userInfo;
    },
    completedRecords() {
      if (this.hasError) return this.staticData.completed;
      return this.recordList.filter(record => record.status === 3).length;
    },
    averageScore() {
      if (this.hasError) return this.staticData.avgScore;
      
      // 只统计评分大于0的记录
      const scores = this.recordList
        .filter(record => record.score !== null && record.score !== undefined && record.score > 0)
        .map(record => record.score);
      
      if (scores.length === 0) return 0;
      return scores.reduce((sum, score) => sum + score, 0) / scores.length;
    },
    displayDoctorCount() {
      return this.hasError ? this.staticData.doctors : (this.doctorList ? this.doctorList.length : 0);
    },
    displayRecordCount() {
      return this.hasError ? this.staticData.records : (this.recordList ? this.recordList.length : 0);
    }
  },
  async mounted() {
    // 等待DOM完全渲染
    this.$nextTick(async () => {
      this.loading = true;
      try {
        await this.loadData();
        // 在数据加载完成后初始化图表
        this.$nextTick(() => {
          this.initCharts();
          this.chartsInitialized = true;
        });
        this.hasError = false;
      } catch (error) {
        // 忽略取消的请求错误
        if (isRequestCanceled(error)) {
          console.log('数据加载请求已取消');
          return;
        }
        
        console.error('加载统计数据失败:', error);
        this.$message.error('加载统计数据失败，使用静态数据显示');
        this.hasError = true;
        this.useStaticData();
      } finally {
        this.loading = false;
      }
      
      window.addEventListener('resize', this.handleResize);
    });
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.handleResize);
    
    // 处理图表销毁
    this.charts.forEach(chart => {
      if (chart) {
        chart.dispose();
      }
    });
    this.charts = [];
    
    // 取消所有未完成的请求
    cancelAllRequests();
  },
  methods: {
    useStaticData() {
      this.recordList = [];
      this.doctorList = [];
      
      this.$nextTick(() => {
        this.initChartsWithStaticData();
      });
    },
    async loadData() {
      try {
        console.log('开始加载数据...');
        const result = await getRecordList({...this.tableParams, ...this.getDateFilter()});
        this.recordList = Array.isArray(result.recordList) ? result.recordList : [];
        console.log('记录数据加载成功:', this.recordList.length);
        
        const doctorResult = await getDoctorList();
        this.doctorList = Array.isArray(doctorResult) ? doctorResult : [];
        console.log('医生数据加载成功:', this.doctorList.length);
        
        this.$nextTick(() => {
          console.log('计算属性更新:', {
            completedRecords: this.completedRecords,
            averageScore: this.averageScore
          });
        });
      } catch (error) {
        if (isRequestCanceled(error)) {
          console.log('数据加载请求已取消');
          return; // 不抛出错误，允许组件继续渲染
        }
        console.error('数据加载失败:', error);
        this.recordList = [];
        this.doctorList = [];
        throw error;
      }
    },
    getDateFilter() {
      if (!this.dateRange || !this.dateRange[0] || !this.dateRange[1]) {
        return {};
      }
      
      const startDate = this.formatDate(this.dateRange[0]);
      const endDate = this.formatDate(this.dateRange[1]);
      
      return {
        startDate,
        endDate
      };
    },
    formatDate(date) {
      const d = new Date(date);
      const year = d.getFullYear();
      const month = String(d.getMonth() + 1).padStart(2, '0');
      const day = String(d.getDate()).padStart(2, '0');
      return `${year}-${month}-${day}`;
    },
    handleDateChange() {
      this.refreshData();
    },
    async refreshData() {
      this.loading = true;
      try {
        await this.loadData();
        
        // 确保DOM更新后再更新图表
        this.$nextTick(() => {
          this.updateCharts();
        });
      } catch (error) {
        if (isRequestCanceled(error)) {
          console.log('刷新请求已取消');
          return;
        }
        console.error('刷新统计数据失败:', error);
        this.$message.error('刷新统计数据失败，请稍后再试');
      } finally {
        this.loading = false;
      }
    },
    handleResize() {
      this.charts.forEach(chart => {
        if (chart) {
          chart.resize();
        }
      });
    },
    initCharts() {
      this.initScoreChart();
      this.initStatusChart();
    },
    updateCharts() {
      this.initScoreChart();
      this.initStatusChart();
    },
    initScoreChart() {
      const doctorScores = {};
      
        this.recordList.forEach(record => {
            let { doctorName, score } = record;
        if (!doctorName) return;
        
            if (!doctorScores[doctorName]) {
          doctorScores[doctorName] = { totalScore: 0, count: 0 };
            }
        
        // 只统计有效评分（大于0的评分）
        if (score !== null && score !== undefined && score > 0) {
            doctorScores[doctorName].totalScore += score;
            doctorScores[doctorName].count++;
        }
      });
      
      const averageScores = {};
        for (const doctorName in doctorScores) {
        // 只计算有评分记录的医生的平均分
        if (doctorScores[doctorName].count > 0) {
          averageScores[doctorName] = (doctorScores[doctorName].totalScore / doctorScores[doctorName].count).toFixed(1);
        }
      }
      
      // 按评分降序排序
      const sortedData = Object.entries(averageScores)
        .sort((a, b) => b[1] - a[1]);
        
      // 只取前10名有评分的医生
      const top10Data = sortedData.slice(0, 10);
      
      const doctorNames = top10Data.map(item => item[0]);
      const scores = top10Data.map(item => item[1]);
      
        const option = {
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          },
          formatter: '{b}: {c}分'
            },
            grid: {
                left: '3%',
          right: '10%',
                bottom: '1%',
          top: '3%',
                containLabel: true
            },
            xAxis: {
          type: 'value',
          name: '评分',
          axisLabel: {
            formatter: '{value}分'
          }
            },
            yAxis: {
                type: 'category',
          data: doctorNames,
          axisLabel: {
            interval: 0,
            rotate: 0,
            formatter: function(value) {
              if (value.length > 6) {
                return value.substring(0, 6) + '...';
              }
              return value;
            }
          }
            },
            series: [
                {
            name: '平均评分',
            type: 'bar',
            data: scores,
            itemStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
                { offset: 0, color: '#83bff6' },
                { offset: 0.5, color: '#188df0' },
                { offset: 1, color: '#188df0' }
              ])
            },
            emphasis: {
              itemStyle: {
                color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
                  { offset: 0, color: '#2378f7' },
                  { offset: 0.7, color: '#2378f7' },
                  { offset: 1, color: '#83bff6' }
                ])
              }
            },
            label: {
              show: true,
              position: 'right',
              formatter: '{c}分'
            }
          }
        ]
      };
      
      this.initChart(this.$refs.scoreChart, option, 'scoreChart');
    },
    initStatusChart() {
      const statusCount = {
        '未查看': 0,
        '已查看': 0,
        '已诊断': 0,
        '诊断已结束': 0
      };
      
      this.recordList.forEach(record => {
        switch(record.status) {
          case 0:
            statusCount['未查看']++;
            break;
          case 1:
            statusCount['已查看']++;
            break;
          case 2:
            statusCount['已诊断']++;
            break;
          case 3:
            statusCount['诊断已结束']++;
            break;
        }
      });
      
        const option = {
        tooltip: {
          trigger: 'item',
          formatter: '{a} <br/>{b}: {c} ({d}%)'
        },
        legend: {
          orient: 'vertical',
          right: 10,
          top: 'center',
          data: Object.keys(statusCount)
        },
        series: [
          {
            name: '诊断状态',
            type: 'pie',
            radius: ['40%', '65%'],
            center: ['40%', '50%'],
            avoidLabelOverlap: false,
            label: {
              show: true,
              position: 'outside',
              formatter: '{b}: {c}',
              fontSize: 12,
              distance: 5
            },
            emphasis: {
              label: {
                show: true,
                fontSize: '14',
                fontWeight: 'bold'
              }
            },
            labelLine: {
              show: true,
              length: 5,
              length2: 5
            },
            data: [
              { value: statusCount['未查看'], name: '未查看', itemStyle: { color: '#e0e0e0' } },
              { value: statusCount['已查看'], name: '已查看', itemStyle: { color: '#ffb74d' } },
              { value: statusCount['已诊断'], name: '已诊断', itemStyle: { color: '#4fc3f7' } },
              { value: statusCount['诊断已结束'], name: '诊断已结束', itemStyle: { color: '#81c784' } }
            ]
          }
        ]
      };
      
      this.initChart(this.$refs.statusChart, option, 'statusChart');
    },
    initChart(chartRef, option, chartName) {
      if (!chartRef) {
        console.warn(`图表DOM不存在: ${chartName}`);
        return;
      }
      
      // 确保之前的图表实例被正确处理
      if (this[chartName]) {
        this[chartName].dispose();
        // 从数组中移除旧实例
        const index = this.charts.indexOf(this[chartName]);
        if (index > -1) {
          this.charts.splice(index, 1);
        }
      }
      
      try {
        // 检查容器尺寸
        if (chartRef.clientWidth === 0 || chartRef.clientHeight === 0) {
          console.warn(`图表容器尺寸为零: ${chartName}`, {
            width: chartRef.clientWidth,
            height: chartRef.clientHeight
          });
          
          // 设置最小高度
          chartRef.style.height = '300px';
        }
        
        this[chartName] = echarts.init(chartRef);
        this[chartName].setOption(option);
        
        // 添加到图表数组
        this.charts.push(this[chartName]);
        console.log(`图表初始化成功: ${chartName}`);
      } catch (error) {
        console.error(`图表初始化失败: ${chartName}`, error);
      }
    },
    initChartsWithStaticData() {
      // 初始化医生评分排名图表
      const doctorScores = {};
      
      for (let i = 0; i < this.staticData.doctors; i++) {
        doctorScores[`医生${i + 1}`] = { totalScore: 0, count: 0 };
      }
      
      const averageScores = {};
      for (const doctorName in doctorScores) {
        averageScores[doctorName] = this.staticData.avgScore.toFixed(1);
      }
      
      // 按评分降序排序
      const sortedData = Object.entries(averageScores)
        .sort((a, b) => b[1] - a[1]);
      
      // 只取前10名医生
      const top10Data = sortedData.slice(0, 10);
      
      const doctorNames = top10Data.map(item => item[0]);
      const scores = top10Data.map(item => item[1]);
      
        const option = {
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          },
          formatter: '{b}: {c}分'
            },
            grid: {
                left: '3%',
          right: '10%',
                bottom: '1%',
          top: '3%',
                containLabel: true
            },
            xAxis: {
          type: 'value',
          name: '评分',
          axisLabel: {
            formatter: '{value}分'
          }
        },
        yAxis: {
                type: 'category',
          data: doctorNames,
          axisLabel: {
            interval: 0,
            rotate: 0,
            formatter: function(value) {
              if (value.length > 6) {
                return value.substring(0, 6) + '...';
              }
              return value;
            }
          }
        },
        series: [
          {
            name: '平均评分',
            type: 'bar',
            data: scores,
            itemStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
                { offset: 0, color: '#83bff6' },
                { offset: 0.5, color: '#188df0' },
                { offset: 1, color: '#188df0' }
              ])
            },
            emphasis: {
              itemStyle: {
                color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
                  { offset: 0, color: '#2378f7' },
                  { offset: 0.7, color: '#2378f7' },
                  { offset: 1, color: '#83bff6' }
                ])
              }
            },
            label: {
              show: true,
              position: 'right',
              formatter: '{c}分'
            }
          }
        ]
      };
      
      this.initChart(this.$refs.scoreChart, option, 'scoreChart');
      
      // 初始化诊断状态分布图表
      const statusCount = {
        '未查看': 0,
        '已查看': 2,
        '已诊断': 3,
        '诊断已结束': this.staticData.completed
      };
      
      const statusOption = {
        tooltip: {
          trigger: 'item',
          formatter: '{a} <br/>{b}: {c} ({d}%)'
        },
        legend: {
          orient: 'vertical',
          right: 10,
          top: 'center',
          data: Object.keys(statusCount)
            },
            series: [
                {
            name: '诊断状态',
            type: 'pie',
            radius: ['40%', '65%'],
            center: ['40%', '50%'],
            avoidLabelOverlap: false,
            label: {
              show: true,
              position: 'outside',
              formatter: '{b}: {c}',
              fontSize: 12,
              distance: 5
            },
            emphasis: {
              label: {
                show: true,
                fontSize: '14',
                fontWeight: 'bold'
              }
            },
            labelLine: {
              show: true,
              length: 5,
              length2: 5
            },
            data: [
              { value: statusCount['未查看'], name: '未查看', itemStyle: { color: '#e0e0e0' } },
              { value: statusCount['已查看'], name: '已查看', itemStyle: { color: '#ffb74d' } },
              { value: statusCount['已诊断'], name: '已诊断', itemStyle: { color: '#4fc3f7' } },
              { value: statusCount['诊断已结束'], name: '诊断已结束', itemStyle: { color: '#81c784' } }
            ]
          }
        ]
      };
      
      this.initChart(this.$refs.statusChart, statusOption, 'statusChart');
    }
  }
};
</script>

<style scoped lang="scss">
:root {
  --border-radius-lg: 8px;
  --shadow-md: 0 4px 12px rgba(0, 0, 0, 0.1);
  --gradient-parchment: linear-gradient(to bottom, #f9f4e8, #f5f0e5);
  --cinnabar-red: #e63946;
  --border-color: #e0e0e0;
  --gradient-cinnabar: linear-gradient(to right, #e63946, #ff6b6b);
  --gradient-chrysanthemum: linear-gradient(to right, #ffb347, #ffcc33);
  --chrysanthemum-yellow: #ffcc33;
}

.statistics-container {
  padding: 5px;
  height: 100%;
  background-image: url('@/assets/images/patterns/cloud-pattern.svg');
  background-size: 300px;
  background-repeat: repeat;
  background-position: center;
  background-attachment: fixed;
  background-opacity: 0.05;
  position: relative;
  overflow: hidden;
}

.statistics-card {
  height: 100%;
  border-radius: var(--border-radius-lg);
  box-shadow: var(--shadow-md);
  background: var(--gradient-parchment);
  border-top: 3px solid var(--cinnabar-red);
  overflow: auto;
  
  .card-header {
    margin-bottom: 5px;
    border-bottom: 1px solid var(--border-color);
    padding: 5px 15px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .header-left {
      display: flex;
      flex-direction: column;
      
      h2 {
        margin: 0;
        color: var(--rhubarb-brown);
        font-size: 18px;
        font-weight: 500;
        position: relative;
        padding-left: 15px;
        
        &::before {
          content: '';
          position: absolute;
          left: 0;
          top: 50%;
          transform: translateY(-50%);
          width: 4px;
          height: 18px;
          background: var(--gradient-cinnabar);
          border-radius: 2px;
        }
      }
      
      .header-decoration {
        display: flex;
        align-items: center;
        margin-top: 3px;
        margin-left: 15px;
        
        .decoration-line {
          height: 2px;
          width: 40px;
          background: var(--gradient-chrysanthemum);
          border-radius: 1px;
        }
        
        .decoration-dot {
          width: 5px;
          height: 5px;
          border-radius: 50%;
          background-color: var(--chrysanthemum-yellow);
          margin-left: 5px;
        }
      }
    }
    
    .header-right {
      display: flex;
      align-items: center;
      gap: 10px;
    }
  }
}

.data-overview {
  margin-bottom: 10px;
  
  .stat-card {
    display: flex;
    align-items: center;
    padding: 10px;
    transition: all 0.3s;
    height: 90px;
    background-color: #fff;
    border-radius: 8px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
    
    &:hover {
      transform: translateY(-3px);
      box-shadow: 0 4px 16px 0 rgba(0, 0, 0, 0.15);
    }
    
    .stat-icon {
      width: 40px;
      height: 40px;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-right: 10px;
      flex-shrink: 0;
      
      i {
        font-size: 22px;
        color: white;
      }
    }
    
    .doctor-icon {
      background: linear-gradient(135deg, #42a5f5, #1976d2);
    }
    
    .record-icon {
      background: linear-gradient(135deg, #66bb6a, #388e3c);
    }
    
    .completed-icon {
      background: linear-gradient(135deg, #ffca28, #ff8f00);
    }
    
    .rating-icon {
      background: linear-gradient(135deg, #ef5350, #c62828);
    }
    
    .stat-content {
      .stat-title {
        font-size: 14px;
        color: #606266;
        margin-bottom: 2px;
      }
      
      .stat-value {
        font-size: 22px;
        font-weight: 600;
        color: #303133;
      }
    }
  }
}

.chart-container {
  .chart-card {
    margin-bottom: 5px;
    
    .chart-header {
    display: flex;
      align-items: center;
      margin-bottom: 5px;
      padding: 0 5px;
      
      h3 {
        margin: 0;
        font-size: 16px;
        font-weight: 500;
        color: var(--rhubarb-brown);
      }
      
      i {
        margin-left: 5px;
        color: var(--text-secondary);
        cursor: pointer;
      }
    }
    
    .chart-body {
      height: 230px;
    }
  }
}

/* 响应式调整 */
@media screen and (max-width: 1200px) {
  .data-overview {
    .stat-card {
      margin-bottom: 15px;
      height: 100px;
    }
  }
  
  .chart-container {
    .chart-card {
      .chart-body {
        height: 220px;
      }
    }
  }
}

@media screen and (max-width: 768px) {
  .statistics-container {
    padding: 5px;
  }
  
  .statistics-card {
    .card-header {
      flex-direction: column;
      align-items: flex-start;
      
      .header-right {
        margin-top: 10px;
        width: 100%;
      }
    }
  }
  
  .data-overview {
    .el-col {
      width: 100%;
    }
    
    .stat-card {
      height: 90px;
    }
  }
  
  .chart-container {
    .chart-card {
      .chart-body {
        height: 200px;
      }
    }
  }
}
</style>