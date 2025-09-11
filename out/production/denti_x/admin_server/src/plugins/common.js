const methods = {
  convertDateFormat: (format, dateText) => {
    switch (format) {
      // YYYY년 월 일 변환
      case 'YYYYMMDDKR': {
        const dateArr = dateText.split('-');
        return `${dateArr[0]}년 ${dateArr[1]}월 ${dateArr[2]}일`;
      }
      // YY년 월 일 변환
      case 'YYMMDDKR': {
        const dateArr = dateText.split('-');
        return `${dateArr[0].substring(2)}년 ${dateArr[1]}월 ${dateArr[2]}일`;
      }
      // 년 월 변환
      case 'YYYYMMKR': {
        const dateArr = dateText.split('-');
        return `${dateArr[0]}년 ${dateArr[1]}월`;
      }
      // 년 월 일 사이 점 추가
      case 'dot': {
        const dateArr = dateText.split('-');
        return `${dateArr[0]}.${dateArr[1]}.${dateArr[2]}`;
      }
      // 년 월 일 시간 변환
      case 'YYYYMMDDAMPMKR': {
        const dateTimeArr = dateText.split(' ');
        const dateArr = dateTimeArr[0].split('-');
        const timeArr = dateTimeArr[1].split(':');
        let timeTxt = '';
        let hour = parseInt(timeArr[0]);
        const ampm = hour < 12 ? '오전' : '오후';
        if (hour === 0) hour = 12; // 12시간제에서는 00시 -> 12시
        if (hour > 12) hour = hour - 12; // 12시간제로 표현
        hour = hour > 9 ? `${hour}` : `0${hour}`;
        timeTxt = `${ampm} ${hour}:${timeArr[1]}`;
        return `${dateArr[0]}.${dateArr[1]}.${dateArr[2]} ${timeTxt}`;
      }
      case 'AMPMKR': {
        const dateTimeArr = dateText.split(' ');
        const timeArr = dateTimeArr[1].split(':');
        let timeTxt = '';
        let hour = parseInt(timeArr[0]);
        const ampm = hour < 12 ? '오전' : '오후';
        if (hour === 0) hour = 12; // 12시간제에서는 00시 -> 12시
        if (hour > 12) hour = hour - 12; // 12시간제로 표현
        hour = hour > 9 ? `${hour}` : `0${hour}`;
        timeTxt = `${ampm} ${hour}:${timeArr[1]}`;
        return `${timeTxt}`;
      }
      case 'dotTime': {
        const dateTimeArr = dateText.split(' ');
        const dateArr = dateTimeArr[0].split('-');
        const timeArr = dateTimeArr[1].split(':');
        return `${dateArr[0]}.${dateArr[1]}.${dateArr[2]} ${timeArr[0]}:${timeArr[1]}`;
      }
      case 'YYYYMMDDDayKR': {
        const dayWeek = ['일', '월', '화', '수', '목', '금', '토'];
        const dateTimeArr = dateText.split(' ');
        const dateArr = dateTimeArr[0].split('-');
        const tempDate = new Date(dateTimeArr[0]);
        return `${dateArr[0]}년 ${dateArr[1]}월 ${dateArr[2]}일(${dayWeek[tempDate.getDay()]})`;
      }
      default:
        return '';
    }
  },
};

export default {
  install(Vue) {
    Vue.prototype.$convertDateFormat = methods.convertDateFormat;
  },
};
