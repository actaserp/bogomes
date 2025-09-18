//Ax5Grid 용 공통 Util (default layout 사용)

'use strict';

var dialog = new ax5.ui.dialog();
var confirmDialog = new ax5.ui.dialog();
var toast = new ax5.ui.toast();
var modal = new ax5.ui.modal();
var mask = new ax5.ui.mask();

var subModal = new ax5.ui.modal();
var subMask = new ax5.ui.mask();

var Ax5GridUtil = {
    setSearchParam: function (_pageNo, _pageRowCount, _sortArr) { // 그리도 조회시 기본 파라미터 (paging 및 데이터정렬 정보 존재시)
        var searchparam = '&start=' + ((_pageNo || 0) * _pageRowCount) + '&length=' + _pageRowCount;
        if (_sortArr !== undefined) {
            $.each(_sortArr, function (idx, value) {
                searchparam += '&order[' + idx + '].column=' + value.key;
                searchparam += '&order[' + idx + '].dir=' + value.orderBy;
            });
        }
        return encodeURI(searchparam);
    },
    setSearchNoPageParam: function (_sortArr) { // 그리도 조회시 기본 파라미터 (paging 및 데이터정렬 정보 존재시)
        var searchparam = '';
        if (_sortArr !== undefined) {
            $.each(_sortArr, function (idx, value) {
                searchparam += '&order[' + idx + '].column=' + value.key;
                searchparam += '&order[' + idx + '].dir=' + value.orderBy;
            });
        }
        return encodeURI(searchparam);
    },
    setDataFiltering: function (_list) { // inline edit모드에서 수정,삭제된 데이터만 filter 하는 기능, 신규도 수정과 타입이 같음
        var returnArr = $.grep(_list, function (value, i) {
            if (value.__deleted__) {
                value.cudMode = 'd';
                return value;
            }
            else if (value.cudMode != 'c' && value.__modified__) {
                value.cudMode = 'u';
                return value;
            }
            else if (value.cudMode == 'c' && value.__modified__) {
                value.cudMode = 'c';
                return value;
            }
            /*
            else if (value.__modified__ && jQuery.type(value.__index) === 'undefined') {
              value.__index = i;
              value.cudMode = 'c';
              return value;
            }*/
        });
        return returnArr;
    }
};

// Ax5 Grid Custom Formatter
var Ax5Grid = {
    init: function () {
        // custom formatter 설정
        ax5.ui.grid.formatter['strAttend'] = function () {
            return ((this.value == 'Y') ? '참석' : '');
        };
        ax5.ui.grid.formatter['strManagerYn'] = function () {
            return ((this.value == 'Y') ? '관리자' : '일반');
        };
        ax5.ui.grid.formatter['strBool'] = function () {
            return ((this.value == 'Y') ? '사용' : '미사용');
        };
        ax5.ui.grid.formatter['strObjType'] = function () {
            return ((this.value == 'M') ? '메뉴' : ((this.value == 'S') ? '서브화면' : ((this.value == 'A') ? 'Ajax' : 'TAB')));
        };
        ax5.ui.grid.formatter['capital'] = function () {
            return this.value.toUpperCase();
        };
    }
};
// Ax5 Grid 기본 생성 function
Ax5Grid.gridBuilder = function () {
    var defaultGridConfig = {
        // 해당 DefaultOption은 화면페이지에서 수정 가능함.
        frozenColumnIndex: 0, // 열 고정
        frozenRowIndex: 0,    // 행 고정
        showLineNumber: false, // 열의 번호 보이기 여부
        showRowSelector: false,  // checkbox(선택) 보이기 여부
        multipleSelect: true, // 여러행 선택 가능 여부 (false시 단독 선택)
        sortable: true, // 모든 컬럼에 정렬 아이콘 표시 (columns에서 컬럼별 소팅여부 재설정 가능)
        multiSort: true, // 다중 정렬 여부
        header: {
            align: 'center',  // 헤더의 기본 정렬
            columnHeight: 38  // 헤더 높이
        },
        body: {
            columnHeight: 33 // body의 기본 높이
        },
        page: {
            navigationItemCount: 10, // 페이지 display 갯수 (1 2 3 4 ...)
            height: 30, // 높이
            display: true,  // 페이징 보이기 여부
            statusDisplay: true,
            firstIcon: '<i class="fa fa-step-backward" aria-hidden="true"></i>',
            prevIcon: '<i class="fa fa-caret-left" aria-hidden="true"></i>',
            nextIcon: '<i class="fa fa-caret-right" aria-hidden="true"></i>',
            lastIcon: '<i class="fa fa-step-forward" aria-hidden="true"></i>'
        }
    };
    return function (_config) {
        var myGridConfig = $.extend(true, {}, defaultGridConfig, _config);
        return new ax5.ui.grid(myGridConfig);
    };
}();

// Ax5 Calendar Language 변경
var Ax5Calendar = {
    init: function () {
        // Overriding ax5.info.months
        //ax5.info.months = ['1월', '2월', '3월', '4월', '5월', '6월', '7월', '8월', '9월', '10월', '11월', '12월'];
        ax5.info.months = i18n.getMonthArrayText();
        // Overriding ax5.info.weekNames
        let days = i18n.getDayArrayText();

        //ax5.info.weekNames = [
        //    { label: '일' },
        //    { label: '월' },
        //    { label: '화' },
        //    { label: '수' },
        //    { label: '목' },
        //    { label: '금' },
        //    { label: '토' }
        //];

        ax5.info.weekNames = [
            { label: days[0] },
            { label: days[1] },
            { label: days[2] },
            { label: days[3] },
            { label: days[4] },
            { label: days[5] },
            { label: days[6] }
        ];
    }
}
var picker = new ax5.ui.picker();
// Ax5 Date picker
// 사용법
// $('#input id').ax5DatePicker({direction:'bottom'...configure});
var Ax5DatePicker = {
    init: function () {
        $.fn.ax5DatePicker = function (_config) {
            var defaultconfig = {
                direction: 'top', // top, bottom
                type: 'date',
                mode: 'day',  // day, month, year
                selectMode: 'day',  // day, month, year
                pattern: 'date' // date, date(month), date(year)
            }
            var myconfig = $.extend(true, {}, defaultconfig, _config);

            var $pickerobj = $(this);
            picker.bind({
                target: $pickerobj,
                direction: myconfig.direction,
                content: {
                    width: 212, //270,
                    margin: 10,
                    type: myconfig.type,
                    config: {
                        mode: myconfig.mode,
                        selectMode: myconfig.selectMode,
                        control: {
                            left: '<i class="fa fa-arrow-left"></i>',  //chevron
                            //yearTmpl: '%s년',
                            //monthTmpl: '%s월',
                            right: '<i class="fa fa-arrow-right"></i>'
                        },
                        lang: {
                            //yearTmpl: "%s년",
                            //months: ['01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12'],
                            //dayTmpl: "%s"
                        },
                        marker: (function () {
                            var marker = {};
                            marker[ax5.util.date(new Date(), { 'return': 'yyyy-MM-dd', 'add': { d: 0 } })] = true;

                            return marker;
                        })()
                    },
                    formatter: {
                        pattern: myconfig.pattern
                    }
                },
                onStateChanged: function () {
                    if (this.state == "open") {
                        var selectedValue = this.self.getContentValue(this.item["$target"]);
                        if (!selectedValue) {
                            this.item.pickerCalendar[0].ax5uiInstance.setSelection([ax5.util.date(new Date(), { 'add': { d: 1 } })]);
                        }
                    }
                }
            });
        }
    }
};

// ax5ui alert,confirm
var Alert = {
    init: function () {
        dialog.setConfig({
            title: i18n.getCommonText('정보'),
            lang: {
                "ok": i18n.getCommonText("확인")
            },
            onStateChanged: function () {
                if (this.state === 'open') {
                    mask.open();

                    // 부모 페이지에 중앙으로 이동 요청
                    parent.postMessage('centerPopup', '*');

                    // 다이얼로그 타이틀에 'X' 버튼 추가
                    const dialogTitle = document.querySelector('.ax-dialog-body');

                    // 기존에 'X' 버튼이 없으면 추가
                    if (!dialogTitle.querySelector('.btn-close')) {
                        const closeButton = document.createElement('img');
                        closeButton.src = '/images/icon/btn-popup-close.svg'; // 이미지 경로
                        closeButton.alt = '닫기';
                        closeButton.className = 'btn-close';
                        closeButton.style.position = 'absolute';
                        closeButton.style.right = '40px';
                        closeButton.style.top = '40px';
                        closeButton.style.border = 'none';
                        closeButton.style.background = 'none';
                        closeButton.style.fontSize = '16px';
                        closeButton.style.cursor = 'pointer';

                        // 'X' 버튼을 클릭했을 때 다이얼로그를 닫기
                        closeButton.addEventListener('click', function () {
                            dialog.close(); // 다이얼로그 닫기
                        });

                        dialogTitle.appendChild(closeButton);
                    }
                }
                else if (this.state === 'close') {
                    mask.close();
                }
            }
        });
        confirmDialog.setConfig({
            // title: i18n.getCommonText('확인'),
            lang: {
                "ok": i18n.getCommonText("확인"),
                "cancel": i18n.getCommonText("취소")
            },
            theme: 'default',
            buttons: [
                {key: "ok", label: i18n.getCommonText("확인")},       // 확인 버튼을 나중에
                {key: "cancel", label: i18n.getCommonText("취소")}  // 취소 버튼을 먼저

            ],
            onStateChanged: function () {
                if (this.state === 'open') {
                    mask.open();

                    // 다이얼로그 타이틀에 'X' 버튼 추가
                    const dialogTitle = document.querySelector('.ax-dialog-body');

                    // 기존에 'X' 버튼이 없으면 추가
                    if (!dialogTitle.querySelector('.btn-close')) {
                        const closeButton = document.createElement('img');
                        closeButton.src = '/images/icon/btn-popup-close.svg'; // 이미지 경로
                        closeButton.alt = '닫기';
                        closeButton.className = 'btn-close';
                        closeButton.style.position = 'absolute';
                        closeButton.style.right = '40px';
                        closeButton.style.top = '40px';
                        closeButton.style.border = 'none';
                        closeButton.style.background = 'none';
                        closeButton.style.fontSize = '16px';
                        closeButton.style.cursor = 'pointer';

                        // 'X' 버튼을 클릭했을 때 다이얼로그를 닫기
                        closeButton.addEventListener('click', function () {
                            confirmDialog.close(); // 다이얼로그 닫기
                        });

                        dialogTitle.appendChild(closeButton);
                    }

                    // 버튼 순서를 변경하는 로직
                    const buttonWrap = document.querySelector('.ax-button-wrap');
                    const okButton = buttonWrap.querySelector('[data-dialog-btn="ok"]');
                    const cancelButton = buttonWrap.querySelector('[data-dialog-btn="cancel"]');

                    // 기존 버튼 순서를 교체
                    if (cancelButton && okButton) {
                        buttonWrap.appendChild(okButton);    // 확인 버튼을 뒤에 추가
                        buttonWrap.appendChild(cancelButton); // 취소 버튼을 먼저 추가
                    }
                }
                else if (this.state === 'close') {
                    mask.close();
                }
            }
        });
    },
    alert: function (_title, _msg, _okCallback) {
        if (_title) dialog.config.title = i18n.getCommonText(_title);
        let message = i18n.getCommonText(_msg);
        setTimeout(() => {
            document.querySelector('[data-dialog-btn="ok"]')?.focus();
        }, 50);
        dialog.alert(Utils.decodingHTMLTag(message), function () {
            if (_okCallback !== undefined) {
                _okCallback();
            }
        });
    },
    confirm: function (_title, _msg, _yesCallback, _noCallback) {
        if (_title) dialog.config.title = i18n.getCommonText(_title);

        let message = i18n.getCommonText(_msg);

        // handleKey를 외부에 선언해서 스코프 공유
        const handleKey = (e) => {
            const okBtn = document.querySelector('[data-dialog-btn="ok"]');
            const cancelBtn = document.querySelector('[data-dialog-btn="cancel"]');

            if (document.activeElement === okBtn && e.key === 'ArrowRight') {
                cancelBtn?.focus();
                e.preventDefault();
            } else if (document.activeElement === cancelBtn && e.key === 'ArrowLeft') {
                okBtn?.focus();
                e.preventDefault();
            }
        };

        confirmDialog.confirm({
            msg: Utils.decodingHTMLTag(message),
            showCancel: true
        }, function () {
            // 확인 또는 취소 시 이벤트 제거
            document.removeEventListener('keydown', handleKey);

            if (this.key == 'ok') {
                _yesCallback();
            } else if (this.key == 'cancel') {
                if (typeof _noCallback !== 'undefined') {
                    _noCallback();
                }
            }
        });

        setTimeout(() => {
            const okBtn = document.querySelector('[data-dialog-btn="ok"]');
            const cancelBtn = document.querySelector('[data-dialog-btn="cancel"]');

            if (okBtn && cancelBtn) {
                okBtn.setAttribute('tabindex', '0');
                cancelBtn.setAttribute('tabindex', '0');

                okBtn.focus();

                // 이벤트 등록
                document.addEventListener('keydown', handleKey);
            }
        }, 100);

    },
    confirmAsync(title, msg) {
        return new Promise((resolve) => {
            Alert.confirm(title, msg, () => resolve(true), () => resolve(false));
        });
    }
};

var Notify = {
    init: function () {
        toast.setConfig({
            icon: '<i class="far fa-bell"></i>',
            containerPosition: 'bottom-right',
            closeIcon: '<i class="fa fa-times"></i>',
            displayTime: 5000,
            lang: {
                "ok": i18n.getCommonText("닫기")
            }
        });
    },
    success: function (msg) {
        let message = i18n.getCommonText(msg);
        toast.push({
            theme: 'success',
            msg: message,
            displayTime : 8000
        }, function () {
        });
    },
    successfnc: function (msg, _func) {
        toast.push({
            theme: 'success',
            msg: msg
        }, _func);
    },
    info: function (msg) {
        toast.push({
            theme: 'info',
            msg: msg
        }, function () {
        });
    },
    warn: function (msg) {
        toast.push({
            theme: 'warning',
            msg: msg
        }, function () {
        });
    },
    error: function (msg) {
        toast.push({
            theme: 'danger',
            msg: msg
        }, function () {
        });
    }
};

var Ax5Modal = {
    open: function (_config) {
        var defaultconfig = {
            width: 1200,
            height: 900,
            method: 'get',
            url: '',
            callbackfn: 'setPopUpResult'
        }
        var myconfig = $.extend(true, {}, defaultconfig, _config);
        var params = '';
        if (myconfig.params) {
            $.each(myconfig.params, function (name, value) {
                if (params != '') {
                    params += '&';
                }
                params += name + '=' + (value);

            });
        }

        modal.open({
            width: ($(window).width() < myconfig.width) ? $(window).width() - 10 : myconfig.width,
            height: ($(window).height() < myconfig.height) ? $(window).height() - 10 : myconfig.height,
            iframe: {
                method: myconfig.method,
                url: myconfig.url,
                param: 'callBack=' + myconfig.callbackfn + '&' + params
            },
            onStateChanged: function () {
                if (this.state === 'open') {
                    mask.open();
                }
                else if (this.state === 'close') {
                    mask.close();
                }
            }
        }, function () {

        });
    }
};

// 미디어 뷰
var Ax5MediaViewer = {
    init: function () {
        $.fn.Ax5MediaViewerSet = function (_config) {
            var $target = $(this);
            var defaultconfig = {
                mData: [], // 이미지/동영상 리스트 데이터 (fileId, fileExt)
                imgext: ['jpg', 'jpeg', 'gif', 'png'], // 이미지 확장자
                vodext: ['mp4', 'avi'], // 동영상 확장자
                vodposterimg: '/img/vod.png', // 저장폴더 경로
                posterWidth: 46,
                posterHeight: 46,
                divClass: 'mediaview_pc',
                vodPath: '/system/vod/player'
            }
            var playlist = [];
            var myconfig = $.extend(true, {}, defaultconfig, _config);

            $target.addClass(myconfig.divClass);

            $.each(myconfig.mData, function () {
                if (myconfig.imgext.indexOf(this.fileExt) > -1) {
                    playlist.push({ image: { src: '/files/filedown/' + this.fileId, poster: '/files/filedown/' + this.fileId } });
                }
                if (myconfig.vodext.indexOf(this.fileExt) > -1) {
                    playlist.push({ video: { html: '<iframe src="' + myconfig.vodPath + '/' + this.fileId + '" frameborder="0"></iframe>', poster: myconfig.vodposterimg } });
                }
            });

            var myViewer = new ax5.ui.mediaViewer({
                target: $target,
                loading: {
                    icon: '<i class="fa fa-spinner fa-pulse fa-2x fa-fw margin-bottom" aria-hidden="true"></i>',
                    text: '<div>Now Loading</div>'
                },
                media: {
                    prevHandle: '<i class="fa fa-chevron-left"></i>',
                    nextHandle: '<i class="fa fa-chevron-right"></i>',
                    width: myconfig.posterWidth, height: myconfig.posterHeight,
                    poster: '<i class="fa fa-youtube-play" style="line-height: 46px;font-size: 20px;"></i>',
                    list: playlist
                }
            });
        }
    }
};

class PopupModalContaniner {
    constructor() {
        this.modal = new ax5.ui.modal();
        this.mask = new ax5.ui.mask();
        this.$content = null;
    }

    open({ width, height, $content }) {
        let _this = this;

        this.$content = $content;
        var config = {
            width: width,
            height: height,
            onStateChanged: function () {
                // 일단 주석처리
                if (this.state === 'open') {
                    _this.mask.open();
                }
                else if (this.state === 'close') {
                    _this.mask.close();
                }
            }
        };

        this.modal.open(config, function () {
            this.$["body-frame"].append($content);
        });


        $content.find('#modal-close, #modal-close2').on('click', function () {
            _this.close();
        });

        return this.modal;
    }

    close() {
        this.modal.close();
    }
}

class PopupDraggable {
    constructor(title_str) {
        let _this = this;
        this.modal = new ax5.ui.modal({
            theme: "primary",
            header: {
                title: title_str,
                btns: {
                    minimize: {
                        label: '<i class="fa fa-minus" style="color:white"></i>',
                        onClick: function () { _this.modal.minimize(); }
                    },
                    restore: {
                        label: '<i class="fa fa-square" style="color:white"></i>',
                        onClick: function () { _this.modal.restore(); }
                    },
                    close: {
                        label: '<i class="fa fa-times" style="color:white"></i>',
                        onClick: function () { _this.modal.close(); }
                    }
                }
            }
        });

        this.mask = new ax5.ui.mask();
        this.$content = null;
        this.fixedWidth = null;
        this.fixedHeight = null;
    }

    adjustPosition(retry = 0) {
        const $root = $(".ax5modal[data-modal-els='root']:last");
        if (!$root.length) return;

        const iframe = window.frameElement;
        if (!iframe) return;

        const rect = iframe.getBoundingClientRect();
        const iframeWidth = rect.width;
        const iframeHeight = rect.height;

        const parentDoc = window.top.document;
        const headerEl = parentDoc.querySelector(".dashboard-layout-header");
        const tabsEl   = parentDoc.querySelector(".page-tabs");

        const headerHeight = (headerEl && headerEl.offsetParent !== null) ? headerEl.offsetHeight : 0;
        const tabsHeight   = (tabsEl && tabsEl.offsetParent !== null) ? tabsEl.offsetHeight : 0;

        const usableHeight = iframeHeight - headerHeight - tabsHeight;

        // 👉 데스크탑 / 태블릿 기준 분기
        const isDesktop = window.innerWidth > 1366;

        // width: 데스크탑은 고정값 우선, 아니면 동적 계산
        const modalWidth = (isDesktop && this.fixedWidth)
            ? this.fixedWidth
            : Math.min(window.innerWidth * 0.96, 1100);

        // height: 데스크탑은 고정값 우선, 아니면 동적 계산
        let modalHeight;
        if (isDesktop && this.fixedHeight) {
            modalHeight = this.fixedHeight;
            if (modalHeight > usableHeight) modalHeight = usableHeight * 0.9;
        } else {
            const bodyFrameH = $root.find("[data-modal-els='body-frame']").outerHeight(true) || 0;
            modalHeight = bodyFrameH + 80;
            if (modalHeight > usableHeight) modalHeight = usableHeight * 0.9;
        }

        const middleTop = (usableHeight - modalHeight) / 2;
        let finalTop = middleTop - modalHeight * 0.1;
        if (finalTop < 0) finalTop = 0;

        const leftPos = (iframeWidth - modalWidth) / 2;

        // 모달 위치/사이즈 적용
        $root.css({
            top: finalTop + "px",
            width: modalWidth + "px",
            height: modalHeight + "px",
            left: leftPos + "px",
            transform: "none"
        });

        // body 높이 강제
        const headerH = $root.find("[data-modal-els='header']").outerHeight(true) || 0;
        $root.find(".ax-modal-body").css({
            height: (modalHeight - headerH) + "px",
            overflow: "auto"
        });
    }



    open({ width, height, $content }) {
        let _this = this;
        this.$content = $content;
        this.fixedWidth = width;
        this.fixedHeight = height;

        var config = {
            width, height,
            onStateChanged: function () {
                if (this.state === "open") {
                    _this.mask.open();
                    // setTimeout(() => _this.adjustPosition(), 50);
                } else if (this.state === "close") {
                    _this.mask.close();
                }
            }
        };

        _this.modal.open(config, function () {
            this.$["body-frame"].append($content);
            setTimeout(() => _this.adjustPosition(), 50);
        });

        $content.find("#modal-close-x, #modal-close-button").on("click", () => {
            _this.close();
        });

        return this.modal;
    }

    close() {
        this.modal.close();
    }
}

$(document).ready(function () {
    Ax5Grid.init();
    Ax5Calendar.init();
    Ax5DatePicker.init();
    Alert.init();
    Notify.init();
    Ax5MediaViewer.init();
});
