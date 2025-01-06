import { createStyles } from 'antd-style';

const useStyles = createStyles(({}) => {
  return {
    searchContentContainer: {
      textAlign: 'center',
    },
    searchContent: {
      maxWidth: 522,
      width: '100%',
      margin: '25px 0',
    },
    cardList: {
      '.ant-list .ant-list-item-content-single': { maxWidth: '100%' },
    },
    pagination: {
      marginTop: '30px',
      display: 'flex',
      justifyContent: 'center',
    },
  };
});

export default useStyles;
